package slimeknights.mantle.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.TranslationHelper;

import java.util.List;

/**
 * Item with automatic tooltip support
 */
public class TooltipItem extends Item {

    public TooltipItem(Settings properties) {
        super(properties);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        TranslationHelper.addOptionalTooltip(stack, tooltip);
        super.appendTooltip(stack, worldIn, tooltip, flagIn);
    }
}
