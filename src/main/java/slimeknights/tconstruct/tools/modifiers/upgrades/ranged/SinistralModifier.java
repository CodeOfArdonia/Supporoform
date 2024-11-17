package slimeknights.tconstruct.tools.modifiers.upgrades.ranged;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class SinistralModifier extends Modifier implements GeneralInteractionModifierHook, EntityInteractionModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.GENERAL_INTERACT, ModifierHooks.ENTITY_INTERACT);
    }

    @Override
    public ActionResult afterEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, LivingEntity target, Hand hand, InteractionSource source) {
        return this.onToolUse(tool, modifier, player, hand, source);
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (source == InteractionSource.LEFT_CLICK && hand == Hand.MAIN_HAND && !tool.isBroken()) {
            NbtCompound heldAmmo = tool.getPersistentData().getCompound(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO);
            if (!heldAmmo.isEmpty()) {
                ModifiableCrossbowItem.fireCrossbow(tool, player, hand, heldAmmo);
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.PASS;
    }
}
