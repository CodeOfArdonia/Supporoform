package slimeknights.tconstruct.tables.block;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Blocks implementing this interface are part of the tinker table tab GUI system
 */
public interface ITabbedBlock {
    /**
     * Open the gui of this block for the given player. Same as {@link slimeknights.mantle.block.InventoryBlock#openGui(PlayerEntity, World, BlockPos)} coincidentally
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean openGui(PlayerEntity player, World world, BlockPos pos);
}
