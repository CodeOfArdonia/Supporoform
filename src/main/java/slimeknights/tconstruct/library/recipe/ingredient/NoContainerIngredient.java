package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.utils.JsonUtils;

import org.jetbrains.annotations.Nullable;

/**
 * Ingredient matching an item with no container item, used to ensure NBT fluid items are empty
 */
public class NoContainerIngredient extends NestedIngredient {
    public static final Identifier ID = TConstruct.getResource("no_container");

    protected NoContainerIngredient(Ingredient nested) {
        super(nested);
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return stack != null && super.test(stack) && !stack.hasCraftingRemainingItem();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public JsonElement toJson() {
        JsonElement nestedElement = this.nested.toJson();
        // if we are a vanilla ingredient, and not an array ingredient, serialize into the ingredient directly
        if (this.nested.isVanilla() && nestedElement.isJsonObject()) {
            JsonObject nestedObject = nestedElement.getAsJsonObject();
            nestedObject.addProperty("type", ID.toString());
            return nestedObject;
        }
        // if we have an array or a type, then serialize nested
        JsonObject json = JsonUtils.withType(ID);
        json.add("match", nestedElement);
        return json;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    public enum Serializer implements IIngredientSerializer<NoContainerIngredient> {
        INSTANCE;

        @Override
        public NoContainerIngredient parse(JsonObject json) {
            // if we have match, parse as a nested object. Without match, just parse the object as vanilla
            Ingredient ingredient;
            if (json.has("match")) {
                ingredient = CraftingHelper.getIngredient(json.get("match"));
            } else {
                ingredient = VanillaIngredientSerializer.INSTANCE.parse(json);
            }
            return new NoContainerIngredient(ingredient);
        }

        @Override
        public NoContainerIngredient parse(PacketByteBuf buffer) {
            return new NoContainerIngredient(Ingredient.fromPacket(buffer));
        }

        @Override
        public void write(PacketByteBuf buffer, NoContainerIngredient ingredient) {
            ingredient.nested.write(buffer);
        }
    }


    /* Static constructors */

    /**
     * Creates an instance from the given nested ingredient
     */
    public static NoContainerIngredient of(Ingredient ingredient) {
        return new NoContainerIngredient(ingredient);
    }

    /**
     * Creates an instance from the given items
     */
    public static NoContainerIngredient of(ItemConvertible... items) {
        return of(Ingredient.ofItems(items));
    }

    /**
     * Creates an instance from the given stacks
     */
    public static NoContainerIngredient of(ItemStack... stacks) {
        return of(Ingredient.ofStacks(stacks));
    }

    /**
     * Creates an instance from the given tag
     */
    public static NoContainerIngredient of(TagKey<Item> tag) {
        return of(Ingredient.fromTag(tag));
    }
}
