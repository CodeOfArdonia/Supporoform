package slimeknights.tconstruct.tools.modifiers.ability.interaction;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.eventbus.api.Event.Result;
import slimeknights.tconstruct.library.events.TinkerToolEvent.ToolShearEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolActionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.ArmorLootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.LootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.behavior.ShowOffhandModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.LootingContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerModifiers;

@RequiredArgsConstructor
public class ShearsAbilityModifier extends NoLevelsModifier implements EntityInteractionModifierHook, ToolActionModifierHook {
    private final int range;
    @Getter
    private final int priority;

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ShowOffhandModule.DISALLOW_BROKEN);
        hookBuilder.addHook(this, ModifierHooks.ENTITY_INTERACT, ModifierHooks.TOOL_ACTION);
    }

    @Override
    public Text getDisplayName(IToolStackView tool, ModifierEntry entry) {
        return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, entry));
    }

    @Override
    public boolean shouldDisplay(boolean advanced) {
        return this.priority > Short.MIN_VALUE;
    }

    /**
     * Swings the given's player hand
     *
     * @param player the current player
     * @param hand   the given hand the tool is in
     */
    protected void swingTool(PlayerEntity player, Hand hand) {
        player.swingHand(hand);
        player.spawnSweepAttackParticles();
    }

    @Override
    public boolean canPerformAction(IToolStackView tool, ModifierEntry modifier, ToolAction toolAction) {
        if (this.isShears(tool)) {
            return toolAction == ToolActions.SHEARS_DIG || toolAction == ToolActions.SHEARS_HARVEST || toolAction == ToolActions.SHEARS_CARVE || toolAction == ToolActions.SHEARS_DISARM;
        }
        return false;
    }

    /**
     * Checks whether the tool counts as shears for modifier logic
     *
     * @param tool Current tool instance
     */
    protected boolean isShears(IToolStackView tool) {
        return true;
    }

    @Override
    public ActionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Entity target, Hand hand, InteractionSource source) {
        if (tool.isBroken() || !tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            return ActionResult.PASS;
        }
        EquipmentSlot slotType = source.getSlot(hand);
        ItemStack stack = player.getEquippedStack(slotType);

        // use looting instead of fortune, as that is our hook with entity access
        // modifier can always use tags or the nullable parameter to distinguish if needed
        LootingContext context = new LootingContext(player, target, null, Util.getSlotType(hand));
        int looting = LootingModifierHook.getLooting(tool, context, player.getStackInHand(hand).getEnchantmentLevel(Enchantments.LOOTING));
        looting = ArmorLootingModifierHook.getLooting(tool, context, looting);
        World world = player.getEntityWorld();
        if (this.isShears(tool) && shearEntity(stack, tool, world, player, target, looting)) {
            boolean broken = ToolDamageUtil.damageAnimated(tool, 1, player, slotType);
            this.swingTool(player, hand);
            runShearHook(tool, player, target, true);

            // AOE shearing
            if (!broken) {
                // if expanded, shear all in range
                int expanded = this.range + tool.getModifierLevel(TinkerModifiers.expanded.getId());
                if (expanded > 0) {
                    for (LivingEntity aoeTarget : player.getEntityWorld().getNonSpectatingEntities(LivingEntity.class, target.getBoundingBox().expand(expanded, 0.25D, expanded))) {
                        if (aoeTarget != player && aoeTarget != target && (!(aoeTarget instanceof ArmorStandEntity) || !((ArmorStandEntity) aoeTarget).isMarker())) {
                            if (shearEntity(stack, tool, world, player, aoeTarget, looting)) {
                                broken = ToolDamageUtil.damageAnimated(tool, 1, player, slotType);
                                runShearHook(tool, player, aoeTarget, false);
                                if (broken) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    /**
     * Runs the hook after shearing an entity
     */
    private static void runShearHook(IToolStackView tool, PlayerEntity player, Entity entity, boolean isTarget) {
        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(ModifierHooks.SHEAR_ENTITY).afterShearEntity(tool, entry, player, entity, isTarget);
        }
    }

    /**
     * Tries to shear an given entity, returns false if it fails and true if it succeeds
     *
     * @param itemStack the current item stack
     * @param world     the current world
     * @param player    the current player
     * @param entity    the entity to try to shear
     * @param fortune   the fortune to apply to the sheared entity
     * @return if the sheering of the entity was performed or not
     */
    private static boolean shearEntity(ItemStack itemStack, IToolStackView tool, World world, PlayerEntity player, Entity entity, int fortune) {
        // event to override entity shearing
        Result result = new ToolShearEvent(itemStack, tool, world, player, entity, fortune).fire();
        if (result != Result.DEFAULT) {
            return result == Result.ALLOW;
        }
        // fallback to forge shearable
        if (entity instanceof IForgeShearable target && target.isShearable(itemStack, world, entity.getBlockPos())) {
            if (!world.isClient) {
                target.onSheared(player, itemStack, world, entity.getBlockPos(), fortune)
                        .forEach(stack -> ModifierUtil.dropItem(entity, stack));
            }
            return true;
        }
        return false;
    }
}
