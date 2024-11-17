package slimeknights.tconstruct.library.modifiers.hook.ranged;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;

import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

import java.util.Collection;

/**
 * Hook for modifiers which wish to modify an arrow before its fired.
 */
public interface ProjectileLaunchModifierHook {
    /**
     * Hook to modify arrow properties after an arrow is fired. Called serverside only, so randomness is safe.
     *
     * @param tool           Bow instance
     * @param modifier       Modifier being used
     * @param shooter        Entity firing the arrow
     * @param projectile     Projectile to modify
     * @param arrow          Arrow to modify as most modifiers wish to change that, will be null for non-arrow projectiles
     * @param persistentData Persistent data instance stored on the arrow to write arbitrary data. Note the modifier list was already written
     * @param primary        If true, this is the primary projectile. Multishot may launch multiple
     */
    void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ProjectileEntity projectile, @Nullable PersistentProjectileEntity arrow, NamespacedNBT persistentData, boolean primary);

    /**
     * Logic to merge multiple hooks into one
     */
    record AllMerger(Collection<ProjectileLaunchModifierHook> modules) implements ProjectileLaunchModifierHook {
        @Override
        public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ProjectileEntity projectile, @Nullable PersistentProjectileEntity arrow, NamespacedNBT persistentData, boolean primary) {
            for (ProjectileLaunchModifierHook module : modules) {
                module.onProjectileLaunch(tool, modifier, shooter, projectile, arrow, persistentData, primary);
            }
        }
    }
}
