package slimeknights.tconstruct.library.modifiers.hook.ranged;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;

import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Collection;

/**
 * Hook fired when an arrow hits an entity
 */
public interface ProjectileHitModifierHook {
    /**
     * Called when a projectile hits an entity
     *
     * @param modifiers      Modifiers from the tool firing this arrow
     * @param persistentData Persistent data on the entity
     * @param modifier       Modifier triggering this hook
     * @param projectile     Projectile that hit the entity
     * @param hit            Hit result
     * @param attacker       Living entity who fired the projectile, null if non-living or not fired
     * @param target         Living target, will be null if not living
     * @return true if the hit should be canceled, preventing vanilla logic
     */
    default boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        return false;
    }

    /**
     * Called when a projectile hits a block
     *
     * @param modifiers      Modifiers from the tool firing this arrow
     * @param persistentData Persistent data on the entity
     * @param modifier       Modifier triggering this hook
     * @param projectile     Projectile that hit the entity
     * @param hit            Hit result
     * @param attacker       Living entity who fired the projectile, null if non-living or not fired
     * @return true if the hit should be canceled
     */
    default boolean onProjectileHitBlock(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, BlockHitResult hit, @Nullable LivingEntity attacker) {
        return false;
    }

    /**
     * Merger that runs all hooks and returns true if any did
     */
    record AllMerger(Collection<ProjectileHitModifierHook> modules) implements ProjectileHitModifierHook {
        @Override
        public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
            boolean ret = false;
            for (ProjectileHitModifierHook module : this.modules) {
                ret |= module.onProjectileHitEntity(modifiers, persistentData, modifier, projectile, hit, attacker, target);
            }
            return ret;
        }

        @Override
        public boolean onProjectileHitBlock(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, BlockHitResult hit, @Nullable LivingEntity attacker) {
            boolean ret = false;
            for (ProjectileHitModifierHook module : this.modules) {
                ret |= module.onProjectileHitBlock(modifiers, persistentData, modifier, projectile, hit, attacker);
            }
            return ret;
        }
    }
}
