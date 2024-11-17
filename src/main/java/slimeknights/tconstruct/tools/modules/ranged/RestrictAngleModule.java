package slimeknights.tconstruct.tools.modules.ranged;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.SingletonLoader;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;

import java.util.List;

/**
 * Modifier to restrict a projectile angle, used also by an event for knockback angle
 */
public enum RestrictAngleModule implements ModifierModule, ProjectileLaunchModifierHook {
    INSTANCE;

    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<RestrictAngleModule>defaultHooks(ModifierHooks.PROJECTILE_LAUNCH);
    public static final SingletonLoader<RestrictAngleModule> LOADER = new SingletonLoader<>(INSTANCE);

    @Override
    public SingletonLoader<RestrictAngleModule> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ProjectileEntity projectile, @Nullable PersistentProjectileEntity arrow, NamespacedNBT persistentData, boolean primary) {
        RestrictAngleModule.clampDirection(projectile.getVelocity(), modifier.getLevel(), projectile);
    }


    /* Helpers */

    /**
     * Shared angle logic
     */
    @SuppressWarnings("SuspiciousNameCombination")
    // mojang uses the angle between X and Z, but parchment named atan2 as the angle between Y and X, makes IDEA mad as it thinks parameters should swap
    public static Vec3d clampDirection(Vec3d direction, int level, @Nullable ProjectileEntity projectile) {
        double oldAngle = MathHelper.atan2(direction.x, direction.z);
        int possibleDirections = Math.max(4, (int) Math.pow(2, 6 - level)); // don't let directions fall below 4, else you start seeing directional biases
        double radianIncrements = 2 * Math.PI / possibleDirections;
        double newAngle = Math.round(oldAngle / radianIncrements) * radianIncrements;
        direction = direction.rotateY((float) (newAngle - oldAngle));
        if (projectile != null) {
            projectile.setVelocity(direction);
            projectile.setYaw((float) (newAngle * 180f / Math.PI));
        }
        return direction;
    }

    /**
     * Called during the living knockback event to apply our effect
     */
    public static void onKnockback(LivingKnockBackEvent event, int level) {
        // start at 4 directions at level 1, then 32, 16, 8, and 4 by level 4, don't go below 4 directions
        Vec3d direction = clampDirection(new Vec3d(event.getRatioX(), 0, event.getRatioZ()), level, null);
        event.setRatioX(direction.x);
        event.setRatioZ(direction.z);
    }
}
