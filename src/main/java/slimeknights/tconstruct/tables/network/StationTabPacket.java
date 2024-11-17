package slimeknights.tconstruct.tables.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkHooks;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.tables.block.ITabbedBlock;

@RequiredArgsConstructor
public class StationTabPacket implements IThreadsafePacket {
    private final BlockPos pos;

    public StationTabPacket(PacketByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public void handleThreadsafe(Context context) {
        ServerPlayerEntity sender = context.getSender();
        if (sender != null) {
            ItemStack heldStack = sender.currentScreenHandler.getCursorStack();
            if (!heldStack.isEmpty()) {
                // set it to empty, so it's doesn't get dropped
                sender.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
            }

            World world = sender.getEntityWorld();
            if (!world.isChunkLoaded(this.pos)) {
                return;
            }
            BlockState state = world.getBlockState(this.pos);
            if (state.getBlock() instanceof ITabbedBlock) {
                ((ITabbedBlock) state.getBlock()).openGui(sender, sender.getEntityWorld(), this.pos);
            } else {
                NamedScreenHandlerFactory provider = state.createScreenHandlerFactory(sender.getEntityWorld(), this.pos);
                if (provider != null) {
                    NetworkHooks.openScreen(sender, provider, this.pos);
                }
            }

            if (!heldStack.isEmpty()) {
                sender.currentScreenHandler.setCursorStack(heldStack);
                TinkerNetwork.getInstance().sendVanillaPacket(sender, new ScreenHandlerSlotUpdateS2CPacket(-1, -1, -1, heldStack));
            }
        }
    }
}
