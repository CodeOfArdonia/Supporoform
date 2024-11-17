package slimeknights.tconstruct.smeltery.network;

import lombok.AllArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent whenever the contents of the smeltery tank change
 */
@AllArgsConstructor
public class SmelteryTankUpdatePacket implements IThreadsafePacket {
    private final BlockPos pos;
    private final List<FluidStack> fluids;

    public SmelteryTankUpdatePacket(PacketByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        int size = buffer.readVarInt();
        this.fluids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.fluids.add(buffer.readFluidStack());
        }
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeVarInt(this.fluids.size());
        for (FluidStack fluid : this.fluids) {
            buffer.writeFluidStack(fluid);
        }
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    private static class HandleClient {
        private static void handle(SmelteryTankUpdatePacket packet) {
            BlockEntityHelper.get(ISmelteryTankHandler.class, MinecraftClient.getInstance().world, packet.pos).ifPresent(te -> te.updateFluidsFromPacket(packet.fluids));
        }
    }
}
