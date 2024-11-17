package slimeknights.mantle.data.predicate.entity;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;

/**
 * Predicate that checks if the given entity has the given enchantment on any of their equipment
 */
public record HasEnchantmentEntityPredicate(Enchantment enchantment) implements LivingEntityPredicate {
    public static final RecordLoadable<HasEnchantmentEntityPredicate> LOADER = RecordLoadable.create(Loadables.ENCHANTMENT.requiredField("enchantment", HasEnchantmentEntityPredicate::enchantment), HasEnchantmentEntityPredicate::new);

    @Override
    public boolean matches(LivingEntity entity) {
        return EnchantmentHelper.getEquipmentLevel(this.enchantment, entity) > 0;
    }

    @Override
    public IGenericLoader<? extends LivingEntityPredicate> getLoader() {
        return LOADER;
    }
}
