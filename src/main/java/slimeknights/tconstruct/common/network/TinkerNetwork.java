package slimeknights.tconstruct.common.network;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.UpdateMaterialsPacket;
import slimeknights.tconstruct.library.materials.stats.UpdateMaterialStatsPacket;
import slimeknights.tconstruct.library.materials.traits.UpdateMaterialTraitsPacket;
import slimeknights.tconstruct.library.modifiers.UpdateModifiersPacket;
import slimeknights.tconstruct.library.modifiers.fluid.UpdateFluidEffectsPacket;
import slimeknights.tconstruct.library.tools.definition.UpdateToolDefinitionDataPacket;
import slimeknights.tconstruct.library.tools.layout.UpdateTinkerSlotLayoutsPacket;
import slimeknights.tconstruct.shared.network.GeneratePartTexturesPacket;
import slimeknights.tconstruct.smeltery.network.ChannelFlowPacket;
import slimeknights.tconstruct.smeltery.network.FaucetActivationPacket;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;
import slimeknights.tconstruct.smeltery.network.SmelteryFluidClickedPacket;
import slimeknights.tconstruct.smeltery.network.SmelteryTankUpdatePacket;
import slimeknights.tconstruct.smeltery.network.StructureErrorPositionPacket;
import slimeknights.tconstruct.smeltery.network.StructureUpdatePacket;
import slimeknights.tconstruct.tables.network.StationTabPacket;
import slimeknights.tconstruct.tables.network.TinkerStationRenamePacket;
import slimeknights.tconstruct.tables.network.TinkerStationSelectionPacket;
import slimeknights.tconstruct.tables.network.UpdateCraftingRecipePacket;
import slimeknights.tconstruct.tables.network.UpdateStationScreenPacket;
import slimeknights.tconstruct.tables.network.UpdateTinkerStationRecipePacket;
import slimeknights.tconstruct.tools.network.EntityMovementChangePacket;
import slimeknights.tconstruct.tools.network.InteractWithAirPacket;
import slimeknights.tconstruct.tools.network.TinkerControlPacket;

import org.jetbrains.annotations.Nullable;

/**
 * Base network class for all tinkers logic
 * <p>
 * In general, if you need to send packets you should use your own network class
 */
public class TinkerNetwork extends NetworkWrapper {
    private static TinkerNetwork instance = null;

    private TinkerNetwork() {
        super(TConstruct.getResource("network"));
    }

    /**
     * Gets the instance of the network
     */
    public static TinkerNetwork getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Attempt to call network getInstance before network is setup");
        }
        return instance;
    }

    /**
     * Called during mod construction to setup the network
     */
    public static void setup() {
        if (instance != null) {
            return;
        }
        instance = new TinkerNetwork();

        // shared
        instance.registerPacket(InventorySlotSyncPacket.class, InventorySlotSyncPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(UpdateNeighborsPacket.class, UpdateNeighborsPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(GeneratePartTexturesPacket.class, GeneratePartTexturesPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(SyncPersistentDataPacket.class, SyncPersistentDataPacket::new, NetworkDirection.PLAY_TO_CLIENT);

        // gadgets
        instance.registerPacket(EntityMovementChangePacket.class, EntityMovementChangePacket::new, NetworkDirection.PLAY_TO_CLIENT);

        // tables
        instance.registerPacket(StationTabPacket.class, StationTabPacket::new, NetworkDirection.PLAY_TO_SERVER);
        instance.registerPacket(TinkerStationRenamePacket.class, TinkerStationRenamePacket::new, NetworkDirection.PLAY_TO_SERVER);
        instance.registerPacket(UpdateCraftingRecipePacket.class, UpdateCraftingRecipePacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(TinkerStationSelectionPacket.class, TinkerStationSelectionPacket::new, NetworkDirection.PLAY_TO_SERVER);
        instance.registerPacket(UpdateTinkerSlotLayoutsPacket.class, UpdateTinkerSlotLayoutsPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(UpdateStationScreenPacket.class, buf -> UpdateStationScreenPacket.INSTANCE, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(UpdateTinkerStationRecipePacket.class, UpdateTinkerStationRecipePacket::new, NetworkDirection.PLAY_TO_CLIENT);

        // tools
        instance.registerPacket(UpdateMaterialsPacket.class, UpdateMaterialsPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(UpdateMaterialStatsPacket.class, UpdateMaterialStatsPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(UpdateMaterialTraitsPacket.class, UpdateMaterialTraitsPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(UpdateToolDefinitionDataPacket.class, UpdateToolDefinitionDataPacket::new, NetworkDirection.PLAY_TO_CLIENT);

        // modifiers
        instance.registerPacket(TinkerControlPacket.class, TinkerControlPacket::read, NetworkDirection.PLAY_TO_SERVER);
        instance.registerPacket(InteractWithAirPacket.class, InteractWithAirPacket::read, NetworkDirection.PLAY_TO_SERVER);
        instance.registerPacket(UpdateModifiersPacket.class, UpdateModifiersPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(UpdateFluidEffectsPacket.class, UpdateFluidEffectsPacket::new, NetworkDirection.PLAY_TO_CLIENT);

        // smeltery
        instance.registerPacket(FluidUpdatePacket.class, FluidUpdatePacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(FaucetActivationPacket.class, FaucetActivationPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(ChannelFlowPacket.class, ChannelFlowPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(SmelteryTankUpdatePacket.class, SmelteryTankUpdatePacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(StructureUpdatePacket.class, StructureUpdatePacket::new, NetworkDirection.PLAY_TO_CLIENT);
        instance.registerPacket(SmelteryFluidClickedPacket.class, SmelteryFluidClickedPacket::new, NetworkDirection.PLAY_TO_SERVER);
        instance.registerPacket(StructureErrorPositionPacket.class, StructureErrorPositionPacket::new, NetworkDirection.PLAY_TO_CLIENT);
    }

    /**
     * Sends a vanilla packet to the given player
     *
     * @param player Player
     * @param packet Packet
     */
    public void sendVanillaPacket(Entity player, Packet<?> packet) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(packet);
        }
    }

    /**
     * Same as {@link #sendToClientsAround(Object, ServerWorld, BlockPos)}, but checks that the world is a serverworld
     *
     * @param msg      Packet to send
     * @param world    World instance
     * @param position Target position
     */
    public void sendToClientsAround(Object msg, @Nullable WorldAccess world, BlockPos position) {
        if (world instanceof ServerWorld server) {
            sendToClientsAround(msg, server, position);
        }
    }

    /**
     * Sends a packet to all entities tracking the given entity
     *
     * @param msg    Packet
     * @param entity Entity to check
     */
    @Override
    public void sendToTrackingAndSelf(Object msg, Entity entity) {
        this.network.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    /**
     * Sends a packet to all entities tracking the given entity
     *
     * @param msg    Packet
     * @param entity Entity to check
     */
    @Override
    public void sendToTracking(Object msg, Entity entity) {
        this.network.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    /**
     * Sends a packet to the whole player list
     *
     * @param targetedPlayer Main player to target, if null uses whole list
     * @param playerList     Player list to use if main player is null
     * @param msg            Message to send
     */
    public void sendToPlayerList(@Nullable ServerPlayerEntity targetedPlayer, PlayerManager playerList, Object msg) {
        if (targetedPlayer != null) {
            sendTo(msg, targetedPlayer);
        } else {
            for (ServerPlayerEntity player : playerList.getPlayerList()) {
                sendTo(msg, player);
            }
        }
    }
}
