package slimeknights.tconstruct.library.modifiers.hook.interaction;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;

import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

/**
 * Hooks for standard interaction logic though blocks. See {@link GeneralInteractionModifierHook} for general interaction and {@link EntityInteractionModifierHook} for entities.
 */
public interface BlockInteractionModifierHook {
    /**
     * Called when interacting with a block before calling the block's interaction method.
     * In general, it's better to use {@link #afterBlockUse(IToolStackView, ModifierEntry, ItemUsageContext, InteractionSource)} for consistency with vanilla behavior.
     *
     * @param tool     Tool performing interaction
     * @param modifier Modifier instance
     * @param context  Usage context
     * @param source   Source of the interaction
     * @return Return PASS or FAIL to allow vanilla handling, any other to stop vanilla and later modifiers from running.
     */
    default ActionResult beforeBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        return ActionResult.PASS;
    }

    /**
     * Called when interacting with a block after calling the block's interaction method.
     *
     * @param tool     Tool performing interaction
     * @param modifier Modifier instance
     * @param context  Usage context
     * @param source   Source of the interaction
     * @return Return PASS or FAIL to allow vanilla handling, any other to stop vanilla and later modifiers from running.
     */
    default ActionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        return ActionResult.PASS;
    }

    /**
     * Logic to merge multiple interaction hooks into one
     */
    record FirstMerger(Collection<BlockInteractionModifierHook> modules) implements BlockInteractionModifierHook {
        @Override
        public ActionResult beforeBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
            ActionResult result = ActionResult.PASS;
            for (BlockInteractionModifierHook module : this.modules) {
                result = module.beforeBlockUse(tool, modifier, context, source);
                if (result.isAccepted()) {
                    return result;
                }
            }
            return result;
        }

        @Override
        public ActionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
            ActionResult result = ActionResult.PASS;
            for (BlockInteractionModifierHook module : this.modules) {
                result = module.afterBlockUse(tool, modifier, context, source);
                if (result.isAccepted()) {
                    return result;
                }
            }
            return result;
        }
    }
}
