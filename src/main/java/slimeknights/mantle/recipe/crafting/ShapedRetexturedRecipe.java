package slimeknights.mantle.recipe.crafting;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.recipe.MantleRecipeSerializers;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RetexturedHelper;

@SuppressWarnings("WeakerAccess")
public class ShapedRetexturedRecipe extends ShapedRecipe {
    /**
     * Ingredient used to determine the texture on the output
     */
    @Getter
    private final Ingredient texture;
    private final boolean matchAll;

    /**
     * Creates a new recipe using an existing shaped recipe
     *
     * @param orig     Shaped recipe to copy
     * @param texture  Ingredient to use for the texture
     * @param matchAll If true, all inputs must match for the recipe to match
     */
    protected ShapedRetexturedRecipe(ShapedRecipe orig, Ingredient texture, boolean matchAll) {
        super(orig.getId(), orig.getGroup(), orig.getCategory(), orig.getWidth(), orig.getHeight(), orig.getIngredients(), orig.output);
        this.texture = texture;
        this.matchAll = matchAll;
    }

    /**
     * Gets the output using the given texture
     *
     * @param texture Texture to use
     * @return Output with texture. Will be blank if the input is not a block
     */
    public ItemStack getResultItem(Item texture, DynamicRegistryManager access) {
        return RetexturedHelper.setTexture(this.getOutput(access).copy(), Block.getBlockFromItem(texture));
    }

    @Override
    public ItemStack craft(RecipeInputInventory craftMatrix, DynamicRegistryManager access) {
        ItemStack result = super.craft(craftMatrix, access);
        Block currentTexture = null;
        for (int i = 0; i < craftMatrix.size(); i++) {
            ItemStack stack = craftMatrix.getStack(i);
            if (!stack.isEmpty() && this.texture.test(stack)) {
                // if the item is the same as the result, copy the texture over
                Block block;
                if (stack.getItem() == result.getItem()) {
                    block = RetexturedHelper.getTexture(stack);
                } else {
                    block = Block.getBlockFromItem(stack.getItem());
                }
                // if no texture, skip
                if (block == Blocks.AIR) {
                    continue;
                }

                // if we have not found a texture yet, store the found block
                if (currentTexture == null) {
                    currentTexture = block;
                    // match all means we must check the rest. If not match all, we can be done
                    if (!this.matchAll) {
                        break;
                    }

                    // if we found a texture before, must match or we do no texture
                } else if (currentTexture != block) {
                    currentTexture = null;
                    break;
                }
            }
        }

        // set the texture if found. No texture will use the fallback
        if (currentTexture != null) {
            return RetexturedHelper.setTexture(result, currentTexture);
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MantleRecipeSerializers.CRAFTING_SHAPED_RETEXTURED;
    }

    public static class Serializer implements LoggingRecipeSerializer<ShapedRetexturedRecipe> {
        @Override
        public ShapedRetexturedRecipe read(Identifier recipeId, JsonObject json) {
            ShapedRecipe recipe = SHAPED.read(recipeId, json);
            Ingredient texture = CraftingHelper.getIngredient(JsonHelper.getElement(json, "texture"));
            boolean matchAll = false;
            if (json.has("match_all")) {
                matchAll = json.get("match_all").getAsBoolean();
            }
            return new ShapedRetexturedRecipe(recipe, texture, matchAll);
        }

        @Nullable
        @Override
        public ShapedRetexturedRecipe fromNetworkSafe(Identifier recipeId, PacketByteBuf buffer) {
            ShapedRecipe recipe = SHAPED.read(recipeId, buffer);
            return recipe == null ? null : new ShapedRetexturedRecipe(recipe, Ingredient.fromPacket(buffer), buffer.readBoolean());
        }

        @Override
        public void toNetworkSafe(PacketByteBuf buffer, ShapedRetexturedRecipe recipe) {
            SHAPED.write(buffer, recipe);
            recipe.texture.write(buffer);
            buffer.writeBoolean(recipe.matchAll);
        }
    }
}
