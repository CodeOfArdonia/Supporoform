package slimeknights.tconstruct.plugin.jei.partbuilder;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;

import org.jetbrains.annotations.Nullable;

/**
 * Ingredient helper for Tinkers patterns
 */
public class PatternIngredientHelper implements IIngredientHelper<Pattern> {
    @Override
    public IIngredientType<Pattern> getIngredientType() {
        return TConstructJEIConstants.PATTERN_TYPE;
    }

    @Override
    public String getDisplayName(Pattern pattern) {
        return pattern.getDisplayName().getString();
    }

    @Override
    public String getUniqueId(Pattern pattern, UidContext context) {
        return pattern.toString();
    }

    @Override
    public Identifier getResourceLocation(Pattern pattern) {
        return pattern;
    }

    @Override
    public Pattern copyIngredient(Pattern pattern) {
        return pattern;
    }

    @Override
    public String getErrorInfo(@Nullable Pattern pattern) {
        if (pattern == null) {
            return "null";
        }
        return pattern.toString();
    }
}
