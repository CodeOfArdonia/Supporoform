package slimeknights.mantle.item;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.TranslationHelper;

import java.util.List;

public class BlockTooltipItem extends BlockItem {
    public BlockTooltipItem(Block blockIn, Settings builder) {
        super(blockIn, builder);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        super.appendTooltip(stack, worldIn, tooltip, flagIn);
        TranslationHelper.addOptionalTooltip(stack, tooltip);
    }
}
