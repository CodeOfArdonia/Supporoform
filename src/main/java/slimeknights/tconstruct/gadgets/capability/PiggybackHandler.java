package slimeknights.tconstruct.gadgets.capability;

import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import slimeknights.tconstruct.common.network.TinkerNetwork;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Capability instance
 * <p>
 * Does not serialize as the world saves the entities already, they just dismounted on logout
 */
@RequiredArgsConstructor
public class PiggybackHandler implements ICapabilityProvider {

    /**
     * Player holding this capability
     */
    @Nullable
    private final PlayerEntity riddenPlayer;
    /**
     * Capability instance for the provider method
     */
    private final LazyOptional<PiggybackHandler> capability = LazyOptional.of(() -> this);
    /**
     * Last found list of passengers, used in serialization and syncing
     */
    private List<Entity> lastPassengers;

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == PiggybackCapability.PIGGYBACK) {
            return this.capability.cast();
        }
        return LazyOptional.empty();
    }

    /**
     * Updates the passengers on the back
     */
    public void updatePassengers() {
        if (this.riddenPlayer != null) {
            // tell the player itself if his riders changed serverside
            if (!this.riddenPlayer.getPassengerList().equals(this.lastPassengers)) {
                if (this.riddenPlayer instanceof ServerPlayerEntity) {
                    TinkerNetwork.getInstance().sendVanillaPacket(this.riddenPlayer, new EntityPassengersSetS2CPacket(this.riddenPlayer));
                }
            }
            this.lastPassengers = this.riddenPlayer.getPassengerList();
        }
    }
}
