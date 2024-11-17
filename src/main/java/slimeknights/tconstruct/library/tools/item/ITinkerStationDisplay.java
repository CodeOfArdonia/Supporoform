package slimeknights.tconstruct.library.tools.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Interface to implement for tools that also display in the tinker station
 */
public interface ITinkerStationDisplay extends ItemConvertible {
    /**
     * The "title" displayed in the GUI
     */
    default Text getLocalizedName() {
        return Text.translatable(asItem().getTranslationKey());
    }

    /**
     * Returns the tool stat information for this tool
     *
     * @param tool        Tool to display
     * @param tooltips    List of tooltips for display
     * @param tooltipFlag Determines the type of tooltip to display
     */
    default List<Text> getStatInformation(IToolStackView tool, @Nullable PlayerEntity player, List<Text> tooltips, TooltipKey key, TooltipContext tooltipFlag) {
        tooltips = TooltipUtil.getDefaultStats(tool, player, tooltips, key, tooltipFlag);
        TooltipUtil.addAttributes(this, tool, player, tooltips, TooltipUtil.SHOW_MELEE_ATTRIBUTES, EquipmentSlot.MAINHAND);
        return tooltips;
    }

    /**
     * Allows making attribute tooltips more efficient by not parsing the tool twice
     *
     * @param tool Tool to check for attributes
     * @param slot Slot with attributes
     * @return Attribute map
     */
    default Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
        return ImmutableMultimap.of();
    }
}
