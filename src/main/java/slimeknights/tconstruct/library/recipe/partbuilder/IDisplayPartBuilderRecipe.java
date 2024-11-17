package slimeknights.tconstruct.library.recipe.partbuilder;

import net.minecraft.registry.Registries;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;

import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Part builder recipes that can show in JEI
 */
public interface IDisplayPartBuilderRecipe extends IPartBuilderRecipe {
    /**
     * Gets the material variant required to craft this recipe
     */
    MaterialVariant getMaterial();

    /**
     * Gets a list of pattern items to display in the pattern slot
     *
     * @return Pattern items
     */
    default List<ItemStack> getPatternItems() {
        return RegistryHelper.getTagValueStream(Registries.ITEM, TinkerTags.Items.DEFAULT_PATTERNS).map(ItemStack::new).toList();
    }
}
