package slimeknights.mantle.network;

import me.pepperbell.simplenetworking.SimpleChannel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.network.packet.ISimplePacket;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A small network implementation/wrapper using AbstractPackets instead of IMessages.
 * Instantiate in your mod class and register your packets accordingly.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NetworkWrapper {
    /**
     * Network instance
     */
    public final SimpleChannel network;
    private int id = 0;
    private static final String PROTOCOL_VERSION = Integer.toString(1);

    /**
     * Creates a new network wrapper
     *
     * @param channelName Unique packet channel name
     */
    public NetworkWrapper(Identifier channelName) {
        this.network = NetworkRegistry.ChannelBuilder
                .named(channelName)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .simpleChannel();
    }

    /**
     * Registers a new {@link ISimplePacket}
     *
     * @param clazz   Packet class
     * @param decoder Packet decoder, typically the constructor
     * @param <MSG>   Packet class type
     */
    public <MSG extends ISimplePacket> void registerPacket(Class<MSG> clazz, Function<PacketByteBuf, MSG> decoder, @Nullable NetworkDirection direction) {
        this.registerPacket(clazz, ISimplePacket::encode, decoder, ISimplePacket::handle, direction);
    }

    /**
     * Registers a new generic packet
     *
     * @param clazz     Packet class
     * @param encoder   Encodes a packet to the buffer
     * @param decoder   Packet decoder, typically the constructor
     * @param consumer  Logic to handle a packet
     * @param direction Network direction for validation. Pass null for no direction
     * @param <MSG>     Packet class type
     */
    public <MSG> void registerPacket(Class<MSG> clazz, BiConsumer<MSG, PacketByteBuf> encoder, Function<PacketByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer, @Nullable NetworkDirection direction) {
        this.network.registerMessage(this.id++, clazz, encoder, decoder, consumer, Optional.ofNullable(direction));
    }


    /* Sending packets */

    /**
     * Sends a packet to the server
     *
     * @param msg Packet to send
     */
    public void sendToServer(Object msg) {
        this.network.sendToServer(msg);
    }

    /**
     * Sends a packet to the given packet distributor
     *
     * @param target  Packet target
     * @param message Packet to send
     */
    public void send(PacketDistributor.PacketTarget target, Object message) {
        this.network.send(target, message);
    }

    /**
     * Sends a vanilla packet to the given entity
     *
     * @param player Player receiving the packet
     * @param packet Packet
     */
    public void sendVanillaPacket(Packet<?> packet, Entity player) {
        if (player instanceof ServerPlayerEntity sPlayer) {
            sPlayer.networkHandler.sendPacket(packet);
        }
    }

    /**
     * Sends a packet to a player
     *
     * @param msg    Packet
     * @param player Player to send
     */
    public void sendTo(Object msg, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            this.sendTo(msg, (ServerPlayerEntity) player);
        }
    }

    /**
     * Sends a packet to a player
     *
     * @param msg    Packet
     * @param player Player to send
     */
    public void sendTo(Object msg, ServerPlayerEntity player) {
        if (!(player instanceof FakePlayer)) {
            this.network.sendTo(msg, player.networkHandler.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    /**
     * Sends a packet to players near a location
     *
     * @param msg         Packet to send
     * @param serverWorld World instance
     * @param position    Position within range
     */
    public void sendToClientsAround(Object msg, ServerWorld serverWorld, BlockPos position) {
        WorldChunk chunk = serverWorld.getWorldChunk(position);
        this.network.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), msg);
    }

    /**
     * Sends a packet to all entities tracking the given entity
     *
     * @param msg    Packet
     * @param entity Entity to check
     */
    public void sendToTrackingAndSelf(Object msg, Entity entity) {
        this.network.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    /**
     * Sends a packet to all entities tracking the given entity
     *
     * @param msg    Packet
     * @param entity Entity to check
     */
    public void sendToTracking(Object msg, Entity entity) {
        this.network.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }
}
