package slimeknights.tconstruct.smeltery.item;

import slimeknights.tconstruct.TConstruct;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Item for creating casts that looks like a tool part
 */
public class DummyMaterialItem extends Item {
    private static final Text DUMMY_TOOL_PART = TConstruct.makeTranslation("item", "dummy_tool_part.tooltip").formatted(Formatting.GRAY);

    public DummyMaterialItem(Settings pProperties) {
        super(pProperties);
    }

    @Override
    public void appendTooltip(ItemStack pStack, @Nullable World pLevel, List<Text> tooltip, TooltipContext pIsAdvanced) {
        tooltip.add(DUMMY_TOOL_PART);
    }
}
