package slimeknights.tconstruct.library.modifiers.hook.special;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Interface that allows another modifier to hook into the shears modifier.
 */
public interface ShearsModifierHook {
    /**
     * Called after a block is successfully harvested
     *
     * @param tool     Tool used in harvesting
     * @param modifier Entry calling this hook
     * @param player   Player shearing
     * @param entity   Entity sheared
     * @param isTarget If true, the sheared entity was targeted. If false, this is AOE shearing
     */
    void afterShearEntity(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Entity entity, boolean isTarget);


    /**
     * Merger that runs all hooks
     */
    record AllMerger(Collection<ShearsModifierHook> modules) implements ShearsModifierHook {
        @Override
        public void afterShearEntity(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Entity entity, boolean isTarget) {
            for (ShearsModifierHook module : modules) {
                module.afterShearEntity(tool, modifier, player, entity, isTarget);
            }
        }
    }
}
