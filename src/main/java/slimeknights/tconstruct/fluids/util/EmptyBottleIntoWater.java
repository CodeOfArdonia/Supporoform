package slimeknights.tconstruct.fluids.util;

import java.util.function.Supplier;

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

public record EmptyBottleIntoWater(Supplier<Item> empty, CauldronBehavior fallback) implements CauldronBehavior {
    @Override
    public ActionResult interact(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack) {
        if (state.get(LeveledCauldronBlock.LEVEL) == 3 || PotionUtil.getPotion(stack) != Potions.WATER) {
            return this.fallback.interact(state, level, pos, player, hand, stack);
        }
        if (!level.isClient) {
            player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(this.empty.get())));
            player.incrementStat(Stats.USE_CAULDRON);
            player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
            level.setBlockState(pos, state.cycle(LeveledCauldronBlock.LEVEL));
            level.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            level.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return ActionResult.success(level.isClient);
    }
}
