package slimeknights.tconstruct.gadgets.block;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/**
 * Extension of cake that utilizes a food instance for properties
 */
public class FoodCakeBlock extends CakeBlock {
    private final FoodComponent food;

    public FoodCakeBlock(Settings properties, FoodComponent food) {
        super(properties);
        this.food = food;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        ActionResult result = this.eatSlice(world, pos, state, player);
        if (result.isAccepted()) {
            return result;
        }
        if (world.isClient() && player.getStackInHand(handIn).isEmpty()) {
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    /**
     * Checks if the given player has all potion effects from the food
     */
    private boolean hasAllEffects(PlayerEntity player) {
        for (Pair<StatusEffectInstance, Float> pair : food.getStatusEffects()) {
            if (pair.getFirst() != null && !player.hasStatusEffect(pair.getFirst().getEffectType())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Eats a single slice of cake if possible
     */
    private ActionResult eatSlice(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!player.canConsume(false) && !food.isAlwaysEdible()) {
            return ActionResult.PASS;
        }
        // repurpose fast eating, will mean no eating if we have the effect
        if (!food.isSnack() && hasAllEffects(player)) {
            return ActionResult.PASS;
        }
        player.incrementStat(Stats.EAT_CAKE_SLICE);
        // apply food stats
        player.getHungerManager().add(this.food.getHunger(), this.food.getSaturationModifier());
        for (Pair<StatusEffectInstance, Float> pair : this.food.getStatusEffects()) {
            if (!world.isClient() && pair.getFirst() != null && world.getRandom().nextFloat() < pair.getSecond()) {
                player.addStatusEffect(new StatusEffectInstance(pair.getFirst()));
            }
        }
        // remove one bite from the cake
        int i = state.get(BITES);
        if (i < 6) {
            world.setBlockState(pos, state.with(BITES, i + 1), 3);
        } else {
            world.removeBlock(pos, false);
        }
        return ActionResult.SUCCESS;
    }
}
