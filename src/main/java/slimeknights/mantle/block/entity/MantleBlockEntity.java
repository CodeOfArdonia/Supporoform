package slimeknights.mantle.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class MantleBlockEntity extends BlockEntity {

    public MantleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isClient() {
        return this.getWorld() != null && this.getWorld().isClient;
    }

    /**
     * Marks the chunk dirty without performing comparator updates (twice!!) or block state checks
     * Used since most of our markDirty calls only adjust TE data
     */
    @SuppressWarnings("deprecation")
    public void setChangedFast() {
        if (this.world != null) {
            if (this.world.isChunkLoaded(this.pos)) {
                this.world.getWorldChunk(this.pos).setNeedsSaving(true);
            }
        }
    }


    /* Syncing */

    /**
     * If true, this TE syncs when {@link net.minecraft.world.World#updateNeighbors(BlockPos, Block) is called
     * Syncs data from {@link #saveSynced(NbtCompound)}
     */
    protected boolean shouldSyncOnUpdate() {
        return false;
    }

    @Override
    @Nullable
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        // number is just used for vanilla, -1 ensures it skips all instanceof checks as its not a vanilla TE
        return this.shouldSyncOnUpdate() ? BlockEntityUpdateS2CPacket.create(this) : null;
    }

    /**
     * Write to NBT that is synced to the client in {@link #toInitialChunkDataNbt()} and in {@link #writeNbt(NbtCompound)}
     *
     * @param nbt NBT
     */
    protected void saveSynced(NbtCompound nbt) {
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        this.saveSynced(nbt);
        return nbt;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        this.saveSynced(nbt);
    }
}
