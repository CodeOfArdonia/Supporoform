package slimeknights.mantle.plugin.jei.entity;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.plugin.jei.MantleJEIConstants;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

/**
 * Handler for working with entity types as ingredients
 */
public class EntityIngredientHelper implements IIngredientHelper<EntityIngredient.EntityInput> {
    @Override
    public IIngredientType<EntityIngredient.EntityInput> getIngredientType() {
        return MantleJEIConstants.ENTITY_TYPE;
    }

    @Override
    public String getDisplayName(EntityIngredient.EntityInput type) {
        return type.type().getName().getString();
    }

    @Override
    public String getUniqueId(EntityIngredient.EntityInput type, UidContext context) {
        return this.getResourceLocation(type).toString();
    }

    @Override
    public Identifier getResourceLocation(EntityIngredient.EntityInput type) {
        return Registries.ENTITY_TYPE.getId(type.type());
    }

    @Override
    public EntityIngredient.EntityInput copyIngredient(EntityIngredient.EntityInput type) {
        return type;
    }

    @Override
    public String getErrorInfo(@Nullable EntityIngredient.EntityInput type) {
        if (type == null) {
            return "null";
        }
        return this.getResourceLocation(type).toString();
    }
}
