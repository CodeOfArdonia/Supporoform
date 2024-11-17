package slimeknights.tconstruct.tools.modifiers.traits.skull;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.explosion.Explosion;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.KeybindInteractModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.effect.NoMilkEffect;

public class SelfDestructiveModifier extends NoLevelsModifier implements KeybindInteractModifierHook, EquipmentChangeModifierHook {
    /**
     * Self damage source
     */
    private static final DamageSource SELF_DESTRUCT = (new DamageSource(TConstruct.prefix("self_destruct"))).bypassArmor().setExplosion();

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ARMOR_INTERACT, ModifierHooks.EQUIPMENT_CHANGE);
    }

    @Override
    public boolean startInteract(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, EquipmentSlot slot, TooltipKey keyModifier) {
        if (player.isSneaking()) {
            TinkerModifiers.selfDestructiveEffect.get().apply(player, 30, 2, true);
            player.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0F, 0.5F);
            return true;
        }
        return false;
    }

    @Override
    public void stopInteract(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, EquipmentSlot slot) {
        player.removeStatusEffect(TinkerModifiers.selfDestructiveEffect.get());
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        context.getEntity().removeEffect(TinkerModifiers.selfDestructiveEffect.get());
    }

    /**
     * Internal potion effect handling the explosion
     */
    public static class SelfDestructiveEffect extends NoMilkEffect {
        public SelfDestructiveEffect() {
            super(StatusEffectCategory.HARMFUL, 0x59D24A, true);
            // make the player slow
            this.addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "68ee3026-1d50-4eb4-914e-a8b05fbfdb71", -0.9f, Operation.MULTIPLY_TOTAL);
        }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            return duration == 1;
        }

        @Override
        public void applyUpdateEffect(LivingEntity living, int amplifier) {
            // effect level is the explosion radius
            if (!living.getWorld().isClient) {
                living.world.explode(living, living.getX(), living.getY(), living.getZ(), amplifier + 1, Explosion.DestructionType.DESTROY);
                living.damage(SELF_DESTRUCT, 99999);
            }
        }
    }
}
