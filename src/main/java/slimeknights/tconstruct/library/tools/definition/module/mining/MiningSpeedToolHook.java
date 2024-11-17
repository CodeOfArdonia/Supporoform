package slimeknights.tconstruct.library.tools.definition.module.mining;

import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.Collection;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;

/**
 * Hook for changing the mining speed beyond effectiveness and tier.
 */
public interface MiningSpeedToolHook {
    /**
     * Updates the mining speed for the tool against the given state
     */
    float modifyDestroySpeed(IToolStackView tool, BlockState state, float speed);

    /**
     * Gets the mining speed for the tool against the given state
     */
    static float getDestroySpeed(ItemStack tool, BlockState state) {
        if (!tool.hasNbt()) {
            return 1;
        }
        return getDestroySpeed(ToolStack.from(tool), state);
    }

    /**
     * Gets the mining speed for the tool against the given state
     */
    static float getDestroySpeed(IToolStackView tool, BlockState state) {
        if (tool.isBroken()) {
            return 0.3f;
        }
        float speed = IsEffectiveToolHook.isEffective(tool, state) ? tool.getStats().get(ToolStats.MINING_SPEED) : 1;
        return Math.max(1, tool.getHook(ToolHooks.MINING_SPEED).modifyDestroySpeed(tool, state, speed));
    }

    /**
     * Merger that runs each hook after the previous
     */
    record ComposeMerger(Collection<MiningSpeedToolHook> hooks) implements MiningSpeedToolHook {
        @Override
        public float modifyDestroySpeed(IToolStackView tool, BlockState state, float speed) {
            for (MiningSpeedToolHook hook : hooks) {
                speed = hook.modifyDestroySpeed(tool, state, speed);
            }
            return speed;
        }
    }
}
