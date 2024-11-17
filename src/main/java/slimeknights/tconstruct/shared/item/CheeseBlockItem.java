package slimeknights.tconstruct.shared.item;

import slimeknights.tconstruct.shared.TinkerCommons;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

public class CheeseBlockItem extends BlockItem {
    public CheeseBlockItem(Block pBlock, Settings pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World level, LivingEntity living) {
        CheeseItem.removeRandomEffect(living);
        ItemStack result = super.finishUsing(stack, level, living);
        if (!(living instanceof PlayerEntity player) || !player.getAbilities().creativeMode) {
            ItemStack cheese = new ItemStack(TinkerCommons.cheeseIngot, 3);
            if (result.isEmpty()) {
                return cheese;
            } else if (living instanceof PlayerEntity player) {
                if (!player.giveItemStack(cheese)) {
                    player.dropItem(cheese, false);
                }
            } else {
                living.dropStack(cheese);
            }

        }
        return result;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World pLevel, List<Text> tooltip, TooltipContext pIsAdvanced) {
        tooltip.add(CheeseItem.TOOLTIP);
    }
}
