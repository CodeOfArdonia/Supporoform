package slimeknights.tconstruct.library.modifiers.hook.special;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Modifier module to detect when the harvest modifier harvested a block.
 */
public interface PlantHarvestModifierHook {
    /**
     * Called after a block is successfully harvested
     *
     * @param tool     Tool used in harvesting
     * @param modifier Entry calling this hook
     * @param context  Item use context, corresponds to the original targeted position
     * @param world    Server world instance
     * @param state    State before it was harvested
     * @param pos      Position that was harvested, may be different from the context
     */
    void afterHarvest(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, ServerWorld world, BlockState state, BlockPos pos);

    /**
     * Merger that runs all hooks
     */
    record AllMerger(Collection<PlantHarvestModifierHook> modules) implements PlantHarvestModifierHook {
        @Override
        public void afterHarvest(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, ServerWorld world, BlockState state, BlockPos pos) {
            for (PlantHarvestModifierHook module : this.modules) {
                module.afterHarvest(tool, modifier, context, world, state, pos);
            }
        }
    }
}
