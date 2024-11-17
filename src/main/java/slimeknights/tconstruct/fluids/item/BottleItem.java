package slimeknights.tconstruct.fluids.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Item that can fill a bottle on right click
 */
public class BottleItem extends Item {
    private final ItemConvertible potion;

    public BottleItem(ItemConvertible potion, Settings props) {
        super(props);
        this.potion = potion;
    }

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand pHand) {
        ItemStack current = player.getStackInHand(pHand);
        BlockHitResult hit = raycast(level, player, RaycastContext.FluidHandling.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            if (!level.canPlayerModifyAt(player, pos)) {
                return TypedActionResult.pass(current);
            }

            if (level.getFluidState(pos).isIn(FluidTags.WATER)) {
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                level.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos);
                player.incrementStat(Stats.USED.getOrCreateStat(this));
                return TypedActionResult.success(ItemUsage.exchangeStack(current, player, PotionUtil.setPotion(new ItemStack(this.potion), Potions.WATER)), level.isClient());
            }
        }
        return TypedActionResult.pass(current);
    }
}
