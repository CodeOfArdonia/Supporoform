package slimeknights.tconstruct.library.modifiers.hook.interaction;

import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Collection;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * Hooks for standard interaction logic though entities. See {@link GeneralInteractionModifierHook} for general interaction and {@link BlockInteractionModifierHook} for blocks.
 */
public interface EntityInteractionModifierHook {
    /**
     * Called when interacting with an entity before standard entity interaction.
     * In general, its better to use {@link #afterEntityUse(IToolStackView, ModifierEntry, PlayerEntity, LivingEntity, Hand, InteractionSource)} for behavior more consistent with vanilla.
     *
     * @param tool     Tool performing interaction
     * @param modifier Modifier instance
     * @param player   Interacting player
     * @param target   Target of interaction
     * @param hand     Hand used for interaction
     * @param source   Source of the interaction
     * @return Return PASS or FAIL to allow vanilla handling, any other to stop vanilla and later modifiers from running.
     */
    default ActionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Entity target, Hand hand, InteractionSource source) {
        return ActionResult.PASS;
    }

    /**
     * Called when interacting with an entity after standard entity interaction.
     *
     * @param tool     Tool performing interaction
     * @param modifier Modifier instance
     * @param player   Interacting player
     * @param target   Target of interaction
     * @param hand     Hand used for interaction
     * @param source   Source of the interaction
     * @return Return PASS or FAIL to allow vanilla handling, any other to stop vanilla and later modifiers from running.
     */
    default ActionResult afterEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, LivingEntity target, Hand hand, InteractionSource source) {
        return ActionResult.PASS;
    }

    /**
     * Logic to merge multiple interaction hooks into one
     */
    record FirstMerger(Collection<EntityInteractionModifierHook> modules) implements EntityInteractionModifierHook {
        @Override
        public ActionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Entity target, Hand hand, InteractionSource source) {
            ActionResult result = ActionResult.PASS;
            for (EntityInteractionModifierHook module : modules) {
                result = module.beforeEntityUse(tool, modifier, player, target, hand, source);
                if (result.isAccepted()) {
                    return result;
                }
            }
            return result;
        }

        @Override
        public ActionResult afterEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, LivingEntity target, Hand hand, InteractionSource source) {
            ActionResult result = ActionResult.PASS;
            for (EntityInteractionModifierHook module : modules) {
                result = module.afterEntityUse(tool, modifier, player, target, hand, source);
                if (result.isAccepted()) {
                    return result;
                }
            }
            return result;
        }
    }


    /**
     * Logic to left click an entity using interaction modifiers
     */
    static boolean leftClickEntity(ItemStack stack, PlayerEntity player, Entity target) {
        ToolStack tool = ToolStack.from(stack);
        if (stack.isIn(TinkerTags.Items.INTERACTABLE_LEFT)) {
            if (!player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                List<ModifierEntry> modifiers = tool.getModifierList();
                // TODO: should this be in the event?
                for (ModifierEntry entry : modifiers) {
                    if (entry.getHook(ModifierHooks.ENTITY_INTERACT).beforeEntityUse(tool, entry, player, target, Hand.MAIN_HAND, InteractionSource.LEFT_CLICK).isAccepted()) {
                        return true;
                    }
                }
                if (target instanceof LivingEntity living) {
                    for (ModifierEntry entry : modifiers) {
                        if (entry.getHook(ModifierHooks.ENTITY_INTERACT).afterEntityUse(tool, entry, player, living, Hand.MAIN_HAND, InteractionSource.LEFT_CLICK).isAccepted()) {
                            return true;
                        }
                    }
                }
            }
        }
        // no left click modifiers? fallback to standard attack
        return ToolAttackUtil.attackEntity(tool, player, target);
    }
}
