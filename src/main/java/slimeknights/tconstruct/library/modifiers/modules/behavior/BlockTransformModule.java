package slimeknights.tconstruct.library.modifiers.modules.behavior;

import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.MutableUseOnContext;

import java.util.Iterator;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Shared logic for interaction actions which transform blocks
 */
public interface BlockTransformModule extends ModifierModule, BlockInteractionModifierHook {
    List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<BlockTransformModule>defaultHooks(ModifierHooks.BLOCK_INTERACT);

    /**
     * If true, disallows targeting the bottom face of the block to transform
     */
    boolean requireGround();

    @Override
    default List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    default ActionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        // tool must not be broken
        if (tool.isBroken() || !tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            return ActionResult.PASS;
        }

        PlayerEntity player = context.getPlayer();
        if (player != null && player.isSneaking()) {
            return ActionResult.PASS;
        }

        // for hoes and shovels, must have nothing but plants above
        if (requireGround() && context.getSide() == Direction.DOWN) {
            return ActionResult.PASS;
        }

        // must actually transform
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState original = world.getBlockState(pos);
        ItemStack stack = context.getStack();
        boolean didTransform = transform(tool, context, original, true);

        // if we made a successful transform, client can stop early
        EquipmentSlot slotType = source.getSlot(context.getHand());
        if (didTransform) {
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }

            // if the tool breaks or it was a campfire, we are done
            if (ToolDamageUtil.damage(tool, 1, player, stack)) {
                if (player != null) {
                    player.sendEquipmentBreakStatus(slotType);
                }
                return ActionResult.CONSUME;
            }
        }

        // AOE transforming, run even if we did not transform the center
        // note we consider anything effective, as hoes are not effective on all tillable blocks
        if (player != null && !tool.isBroken()) {
            int totalTransformed = 0;
            Iterator<BlockPos> aoePos = tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, stack, player, original, world, pos, context.getSide(), AreaOfEffectIterator.AOEMatchType.TRANSFORM).iterator();
            if (aoePos.hasNext()) {
                MutableUseOnContext offsetContext = new MutableUseOnContext(context);
                do {
                    BlockPos newPos = aoePos.next();
                    if (pos.equals(newPos)) {
                        continue;
                    }

                    // try interacting with the new position
                    offsetContext.setOffsetPos(newPos);

                    BlockState newTarget = world.getBlockState(newPos);

                    // limit to playing 40 sounds, that's more than enough for most transforms
                    if (transform(tool, offsetContext, newTarget, totalTransformed < 40)) {
                        totalTransformed++;
                        didTransform = true;

                        if (world.isClient) {
                            break;
                        }

                        // stop if the tool broke
                        if (ToolDamageUtil.damageAnimated(tool, 1, player, slotType)) {
                            break;
                        }
                    }
                } while (aoePos.hasNext());

                // sweep attack if we transformed any
                if (totalTransformed > 0) {
                    player.spawnSweepAttackParticles();
                }
            }
        }

        // if anything happened, return success
        return didTransform ? ActionResult.success(world.isClient) : ActionResult.PASS;
    }

    /**
     * Applies this transformation
     */
    boolean transform(IToolStackView tool, ItemUsageContext context, BlockState original, boolean playSound);
}
