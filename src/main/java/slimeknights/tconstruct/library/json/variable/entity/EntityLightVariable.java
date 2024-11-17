package slimeknights.tconstruct.library.json.variable.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.LightType;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.library.json.TinkerLoadables;

/**
 * Gets the light level at the entity position
 */
public record EntityLightVariable(LightType lightLayer) implements EntityVariable {
    public static final RecordLoadable<EntityLightVariable> LOADER = RecordLoadable.create(TinkerLoadables.LIGHT_LAYER.requiredField("light_layer", EntityLightVariable::lightLayer), EntityLightVariable::new);

    @Override
    public float getValue(LivingEntity entity) {
        return entity.getWorld().getLightLevel(this.lightLayer, entity.getBlockPos());
    }

    @Override
    public IGenericLoader<? extends EntityVariable> getLoader() {
        return LOADER;
    }
}
