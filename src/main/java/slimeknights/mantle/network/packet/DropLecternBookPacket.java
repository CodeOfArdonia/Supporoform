package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Packet to drop the book as item from lectern
 */
@AllArgsConstructor
public class DropLecternBookPacket implements IThreadsafePacket {
    private final BlockPos pos;

    public DropLecternBookPacket(PacketByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleThreadsafe(Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player == null) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        if (!world.isChunkLoaded(this.pos)) {
            return;
        }

        BlockState state = world.getBlockState(this.pos);

        if (state.getBlock() instanceof LecternBlock && state.get(LecternBlock.HAS_BOOK)) {
            BlockEntity te = world.getBlockEntity(this.pos);
            if (te instanceof LecternBlockEntity lecternTe) {
                ItemStack book = lecternTe.getBook().copy();
                if (!book.isEmpty()) {
                    if (!player.giveItemStack(book)) {
                        player.dropItem(book, false, false);
                    }

                    lecternTe.clear();

                    // fix lectern state
                    world.setBlockState(this.pos, state.with(LecternBlock.POWERED, false).with(LecternBlock.HAS_BOOK, false), 3);
                    world.updateNeighborsAlways(this.pos.down(), state.getBlock());
                }
            }
        }
    }
}
