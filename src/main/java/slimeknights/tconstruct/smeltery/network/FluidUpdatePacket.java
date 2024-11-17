package slimeknights.tconstruct.smeltery.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;

public class FluidUpdatePacket implements IThreadsafePacket {

    protected final BlockPos pos;
    protected final FluidStack fluid;

    public FluidUpdatePacket(BlockPos pos, FluidStack fluid) {
        this.pos = pos;
        this.fluid = fluid;
    }

    public FluidUpdatePacket(PacketByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.fluid = buffer.readFluidStack();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeFluidStack(this.fluid);
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    /**
     * Interface to implement for anything wishing to receive fluid updates
     */
    public interface IFluidPacketReceiver {

        /**
         * Updates the current fluid to the specified value
         *
         * @param fluid New fluidstack
         */
        void updateFluidTo(FluidStack fluid);
    }

    /**
     * Safely runs client side only code in a method only called on client
     */
    private static class HandleClient {
        private static void handle(FluidUpdatePacket packet) {
            BlockEntityHelper.get(IFluidPacketReceiver.class, MinecraftClient.getInstance().world, packet.pos).ifPresent(te -> te.updateFluidTo(packet.fluid));
        }
    }
}
