package slimeknights.tconstruct.library.json.variable.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;

/**
 * Gets the level of the mob effect on an entity
 */
public record EntityEffectLevelVariable(StatusEffect effect) implements EntityVariable {
    public static final RecordLoadable<EntityEffectLevelVariable> LOADER = RecordLoadable.create(Loadables.MOB_EFFECT.requiredField("effect", EntityEffectLevelVariable::effect), EntityEffectLevelVariable::new);

    @Override
    public float getValue(LivingEntity entity) {
        StatusEffectInstance instance = entity.getStatusEffect(this.effect);
        if (instance != null) {
            return instance.getAmplifier() + 1;
        }
        return 0;
    }

    @Override
    public IGenericLoader<? extends EntityVariable> getLoader() {
        return LOADER;
    }
}
