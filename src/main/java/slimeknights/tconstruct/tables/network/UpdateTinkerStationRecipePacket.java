package slimeknights.tconstruct.tables.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;

import java.util.Optional;

/**
 * Packet to send the current crafting recipe to a player who opens the tinker station
 */
public class UpdateTinkerStationRecipePacket implements IThreadsafePacket {
    private final BlockPos pos;
    private final Identifier recipe;

    public UpdateTinkerStationRecipePacket(BlockPos pos, ITinkerStationRecipe recipe) {
        this.pos = pos;
        this.recipe = recipe.getId();
    }

    public UpdateTinkerStationRecipePacket(PacketByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.recipe = buffer.readIdentifier();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeIdentifier(this.recipe);
    }

    @Override
    public void handleThreadsafe(Context context) {
        HandleClient.handle(this);
    }

    /**
     * Safely runs client side only code in a method only called on client
     */
    private static class HandleClient {
        private static void handle(UpdateTinkerStationRecipePacket packet) {
            World world = MinecraftClient.getInstance().world;
            if (world != null) {
                Optional<ITinkerStationRecipe> recipe = RecipeHelper.getRecipe(world.getRecipeManager(), packet.recipe, ITinkerStationRecipe.class);

                // if the screen is open, use that to get the TE and update the screen
                boolean handled = false;
                if (MinecraftClient.getInstance().currentScreen instanceof TinkerStationScreen stationScreen) {
                    TinkerStationBlockEntity te = stationScreen.getTileEntity();
                    if (te.getPos().equals(packet.pos)) {
                        recipe.ifPresent(te::updateRecipe);
                        stationScreen.updateDisplay();
                        handled = true;
                    }
                }
                // if the wrong screen is open or no screen, use the tile directly
                if (!handled) {
                    recipe.ifPresent(r -> BlockEntityHelper.get(TinkerStationBlockEntity.class, world, packet.pos).ifPresent(te -> te.updateRecipe(r)));
                }
            }
        }
    }
}
