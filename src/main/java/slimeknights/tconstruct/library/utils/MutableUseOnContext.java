package slimeknights.tconstruct.library.utils;

import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable use on context that can easily be moved to another location
 */
public class MutableUseOnContext extends ItemUsageContext {
    private final BlockPos.Mutable offsetPos;
    @Getter
    private Vec3d clickedLocation;

    public MutableUseOnContext(ItemUsageContext base) {
        this(base.getWorld(), base.getPlayer(), base.getHand(), base.getStack(), base.getHitResult());
    }

    public MutableUseOnContext(World pLevel, @Nullable PlayerEntity pPlayer, Hand pHand, ItemStack pItemStack, BlockHitResult pHitResult) {
        super(pLevel, pPlayer, pHand, pItemStack, pHitResult);
        this.offsetPos = super.getBlockPos().mutableCopy();
        this.clickedLocation = super.getHitPos();
    }

    @Override
    public BlockPos getBlockPos() {
        return this.offsetPos;
    }

    /**
     * Sets the offset position
     */
    public void setOffsetPos(BlockPos offset) {
        this.clickedLocation = this.clickedLocation.add(
                offset.getX() - this.offsetPos.getX(),
                offset.getY() - this.offsetPos.getY(),
                offset.getZ() - this.offsetPos.getZ());
        this.offsetPos.set(offset);
    }
}
