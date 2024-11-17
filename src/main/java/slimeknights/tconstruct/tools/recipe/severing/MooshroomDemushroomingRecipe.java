package slimeknights.tconstruct.tools.recipe.severing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.MooshroomEntity.Type;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;

/**
 * Recipe to deshroom a mooshroom, taking brown into account
 */
public class MooshroomDemushroomingRecipe extends SeveringRecipe {
    public MooshroomDemushroomingRecipe(Identifier id) {
        super(id, EntityIngredient.of(EntityType.MOOSHROOM), ItemOutput.fromItem(Items.RED_MUSHROOM, 5));
    }

    @Override
    public ItemStack getOutput(Entity entity) {
        if (entity instanceof MooshroomEntity mooshroom) {
            if (!mooshroom.isBaby()) {
                return new ItemStack(mooshroom.getMushroomType() == Type.BROWN ? Items.BROWN_MUSHROOM : Items.RED_MUSHROOM, 5);
            }
        }
        return ItemStack.EMPTY;
    }
}
