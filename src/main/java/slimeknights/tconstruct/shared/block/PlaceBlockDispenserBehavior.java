package slimeknights.tconstruct.shared.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Dispenser behavior that places a block
 */
public class PlaceBlockDispenserBehavior extends FallibleItemDispenserBehavior {
    public static PlaceBlockDispenserBehavior INSTANCE = new PlaceBlockDispenserBehavior();

    private PlaceBlockDispenserBehavior() {
    }

    @Override
    protected ItemStack dispenseSilently(BlockPointer source, ItemStack stack) {
        World level = source.getWorld();
        BlockPos target = source.getPos().offset(source.getBlockState().get(DispenserBlock.FACING));
        if (level.isAir(target) && stack.getItem() instanceof BlockItem blockItem) {
            if (!level.isClient) {
                Block block = blockItem.getBlock();
                // could use getPlacementState, but that requires a context, not worth creating
                BlockState state = block.getDefaultState();
                level.setBlockState(target, state, Block.NOTIFY_ALL);
                if (level.getBlockState(target).isOf(block)) {
                    BlockItem.writeNbtToBlockEntity(level, null, target, stack);
                    block.onPlaced(level, target, state, null, stack);
                }
                level.emitGameEvent(null, GameEvent.BLOCK_PLACE, target);
                BlockSoundGroup sound = state.getSoundGroup();
                level.playSound(null, target, state.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            }
            stack.decrement(1);
            this.setSuccess(true);
        }
        return stack;
    }
}
