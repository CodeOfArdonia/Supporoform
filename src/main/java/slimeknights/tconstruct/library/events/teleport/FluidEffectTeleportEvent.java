package slimeknights.tconstruct.library.events.teleport;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import slimeknights.tconstruct.library.utils.TeleportHelper.ITeleportEventFactory;

/**
 * Event fired when an entity teleports via the spilling effect
 */
@Cancelable
public class FluidEffectTeleportEvent extends EntityTeleportEvent {
    public static final ITeleportEventFactory TELEPORT_FACTORY = FluidEffectTeleportEvent::new;

    public FluidEffectTeleportEvent(Entity entity, double targetX, double targetY, double targetZ) {
        super(entity, targetX, targetY, targetZ);
    }
}