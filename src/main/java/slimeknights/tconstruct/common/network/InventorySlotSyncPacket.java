package slimeknights.tconstruct.common.network;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;

public class InventorySlotSyncPacket implements IThreadsafePacket {

    public final ItemStack itemStack;
    public final int slot;
    public final BlockPos pos;

    public InventorySlotSyncPacket(ItemStack itemStack, int slot, BlockPos pos) {
        this.itemStack = itemStack;
        this.slot = slot;
        this.pos = pos;
    }

    public InventorySlotSyncPacket(PacketByteBuf buffer) {
        this.itemStack = buffer.readItemStack();
        this.slot = buffer.readShort();
        this.pos = buffer.readBlockPos();
    }

    @Override
    public void encode(PacketByteBuf packetBuffer) {
        packetBuffer.writeItemStack(this.itemStack);
        packetBuffer.writeShort(this.slot);
        packetBuffer.writeBlockPos(this.pos);
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    /**
     * Safely runs client side only code in a method only called on client
     */
    private static class HandleClient {
        private static void handle(InventorySlotSyncPacket packet) {
            World world = MinecraftClient.getInstance().world;
            if (world != null) {
                BlockEntity te = world.getBlockEntity(packet.pos);
                if (te != null) {
                    te.getCapability(ForgeCapabilities.ITEM_HANDLER)
                            .filter(cap -> cap instanceof IItemHandlerModifiable)
                            .ifPresent(cap -> {
                                ((IItemHandlerModifiable) cap).setStackInSlot(packet.slot, packet.itemStack);
                                //noinspection ConstantConditions
                                Minecraft.getInstance().levelRenderer.blockChanged(null, packet.pos, null, null, 0);
                            });
                }
            }
        }
    }
}
