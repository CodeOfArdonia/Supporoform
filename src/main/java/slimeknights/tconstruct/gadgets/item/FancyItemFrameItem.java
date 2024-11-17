package slimeknights.tconstruct.gadgets.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class FancyItemFrameItem extends Item {

    private final TriFunction<? extends AbstractDecorationEntity, World, BlockPos, Direction> entityProvider;

    public FancyItemFrameItem(Settings props, TriFunction<? extends AbstractDecorationEntity, World, BlockPos, Direction> entityProvider) {
        super(props);
        this.entityProvider = entityProvider;
    }

    /**
     * Called when this item is used when targetting a Block
     */
    @Override
    @NotNull
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos pos = context.getBlockPos();
        Direction facing = context.getSide();
        BlockPos placeLocation = pos.offset(facing);
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        if (player != null && !this.canPlace(player, facing, stack, placeLocation)) {
            return ActionResult.FAIL;
        }

        World world = context.getWorld();
        AbstractDecorationEntity frame = this.entityProvider.apply(world, placeLocation, facing);
        NbtCompound tag = stack.getNbt();
        if (tag != null) {
            EntityType.loadFromEntityNbt(world, player, frame, tag);
        }

        if (frame.canStayAttached()) {
            if (!world.isClient) {
                frame.onPlace();
                world.spawnEntity(frame);
            }
            stack.decrement(1);
            return ActionResult.success(world.isClient);
        }
        return ActionResult.CONSUME;
    }

    private boolean canPlace(PlayerEntity player, Direction facing, ItemStack stack, BlockPos pos) {
        return !player.getWorld().isOutOfHeightLimit(pos) && player.canPlaceOn(pos, facing, stack);
    }

    @FunctionalInterface
    public interface TriFunction<R, T, U, V> {
        R apply(T t, U u, V v);
    }
}
