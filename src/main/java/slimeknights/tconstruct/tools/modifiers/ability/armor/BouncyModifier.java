package slimeknights.tconstruct.tools.modifiers.ability.armor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorLevelModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.utils.SlimeBounceHandler;

public class BouncyModifier extends NoLevelsModifier {
    private static final TinkerDataKey<Integer> BOUNCY = TConstruct.createKey("bouncy");

    public BouncyModifier() {
        // TODO: move this out of constructor to generalized logic
        MinecraftForge.EVENT_BUS.addListener(BouncyModifier::onFall);
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(new ArmorLevelModule(BOUNCY, false, null));
    }

    /**
     * Called when an entity lands to handle the event
     */
    private static void onFall(LivingFallEvent event) {
        LivingEntity living = event.getEntity();
        // using fall distance as the event distance could be reduced by jump boost
        if (living == null || (living.getVelocity().y > -0.3 && living.fallDistance < 3)) {
            return;
        }
        // can the entity bounce?
        if (ArmorLevelModule.getLevel(living, BOUNCY) == 0) {
            return;
        }

        // reduced fall damage when crouching
        if (living.bypassesLandingEffects()) {
            event.setDamageMultiplier(0.5f);
            return;
        } else {
            event.setDamageMultiplier(0.0f);
        }

        // server players behave differently than non-server players, they have no velocity during the event, so we need to reverse engineer it
        Vec3d motion = living.getVelocity();
        if (living instanceof ServerPlayerEntity) {
            // velocity is lost on server players, but we dont have to defer the bounce
            double gravity = living.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get());
            double time = Math.sqrt(living.fallDistance / gravity);
            double velocity = gravity * time;
            living.setVelocity(motion.x / 0.975f, velocity, motion.z / 0.975f);
            living.velocityModified = true;

            // preserve momentum
            SlimeBounceHandler.addBounceHandler(living);
        } else {
            // for non-players, need to defer the bounce
            // only slow down half as much when bouncing
            float factor = living.fallDistance < 2 ? -0.7f : -0.9f;
            living.setVelocity(motion.x / 0.975f, motion.y * factor, motion.z / 0.975f);
            SlimeBounceHandler.addBounceHandler(living, living.getVelocity());
        }
        // update airborn status
        event.setDistance(0.0F);
        if (!living.getWorld().isClient) {
            living.velocityDirty = true;
            event.setCanceled(true);
            living.setOnGround(false); // need to be on ground for server to process this event
        }
        living.playSound(Sounds.SLIMY_BOUNCE.getSound(), 1f, 1f);
    }
}
