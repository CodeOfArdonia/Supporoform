package slimeknights.tconstruct.tools.recipe.severing;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.tools.TinkerModifiers;

/**
 * Beheading recipe that sets player skin
 */
public class PlayerBeheadingRecipe extends SeveringRecipe {
    public PlayerBeheadingRecipe(Identifier id) {
        super(id, EntityIngredient.of(EntityType.PLAYER), ItemOutput.fromItem(Items.PLAYER_HEAD));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.playerBeheadingSerializer.get();
    }

    @Override
    public ItemStack getOutput(Entity entity) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        if (entity instanceof PlayerEntity) {
            GameProfile gameprofile = ((PlayerEntity) entity).getGameProfile();
            stack.getOrCreateNbt().put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), gameprofile));
        }
        return stack;
    }
}
