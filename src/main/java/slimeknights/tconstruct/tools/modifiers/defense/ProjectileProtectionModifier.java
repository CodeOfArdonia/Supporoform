package slimeknights.tconstruct.tools.modifiers.defense;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.tag.DamageTypeTags;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.data.ModifierMaxLevel;
import slimeknights.tconstruct.library.modifiers.modules.armor.ProtectionModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;

import java.util.UUID;

public class ProjectileProtectionModifier extends AbstractProtectionModifier<ModifierMaxLevel> {
    private static final UUID ATTRIBUTE_UUID = UUID.fromString("6f030b1e-e9e1-11ec-8fea-0242ac120002");
    /**
     * Entity data key for the data associated with this modifier
     */
    private static final ComputableDataKey<ModifierMaxLevel> PROJECTILE_DATA = TConstruct.createKey("projectile_protection", ModifierMaxLevel::new);

    public ProjectileProtectionModifier() {
        super(PROJECTILE_DATA, true);
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ProtectionModule.builder().sources(DamageSourcePredicate.CAN_PROTECT, DamageSourcePredicate.simple(source -> source.isIn(DamageTypeTags.IS_PROJECTILE))).eachLevel(2.5f));
    }

    @Override
    protected void set(ModifierMaxLevel data, EquipmentSlot slot, float scaledLevel, EquipmentChangeContext context) {
        float oldMax = data.getMax();
        super.set(data, slot, scaledLevel, context);
        float newMax = data.getMax();
        // 5% bonus attack speed for the largest level
        if (oldMax != newMax) {
            EntityAttributeInstance instance = context.getEntity().getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
            if (instance != null) {
                instance.removeModifier(ATTRIBUTE_UUID);
                if (newMax != 0) {
                    instance.addTemporaryModifier(new EntityAttributeModifier(ATTRIBUTE_UUID, "tconstruct.melee_protection", 0.05 * newMax, Operation.ADDITION));
                }
            }
        }
    }
}
