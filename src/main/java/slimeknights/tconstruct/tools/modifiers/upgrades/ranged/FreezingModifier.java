package slimeknights.tconstruct.tools.modifiers.upgrades.ranged;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;

public class FreezingModifier extends Modifier implements ProjectileHitModifierHook, MeleeHitModifierHook, OnAttackedModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.PROJECTILE_HIT, ModifierHooks.ON_ATTACKED);
    }

    /**
     * Freezes the entity
     */
    private void apply(int level, @Nullable Entity target) {
        if (target != null && target.canFreeze()) {
            target.setFrozenTicks(Math.max(target.getMinFreezeDamageTicks(), target.getFrozenTicks()) + (level + 1) * 80);
            target.setFireTicks(0);
        }
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        this.apply(modifier.getLevel(), context.getLivingTarget());
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        this.apply(modifier.getLevel(), target);
        return false;
    }

    @Override
    public void onAttacked(IToolStackView tool, ModifierEntry modifier, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float amount, boolean isDirectDamage) {
        // this works like vanilla, damage is capped due to the hurt immunity mechanics, so if multiple pieces apply thorns between us and vanilla, damage is capped at 4
        Entity attacker = source.getAttacker();
        if (isDirectDamage && attacker != null && attacker.canFreeze()) {
            // 15% chance of working per level, doubled bonus on shields
            int level = modifier.getLevel();
            if (slotType.getType() == Type.HAND) {
                level *= 2;
            }
            if (RANDOM.nextFloat() < (level * 0.15f)) {
                this.apply(level, attacker);
                ToolDamageUtil.damageAnimated(tool, 1, context.getEntity(), slotType);
            }
        }
    }
}
