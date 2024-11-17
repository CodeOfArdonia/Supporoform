package slimeknights.tconstruct.fluids.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public record FillBottle(Item filled) implements CauldronBehavior {
    @Override
    public ActionResult interact(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack) {
        if (!level.isClient) {
            player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, PotionUtil.setPotion(new ItemStack(this.filled), Potions.WATER)));
            player.incrementStat(Stats.USE_CAULDRON);
            player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
            LeveledCauldronBlock.decrementFluidLevel(state, level, pos);
            level.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            level.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return ActionResult.success(level.isClient);
    }
}
