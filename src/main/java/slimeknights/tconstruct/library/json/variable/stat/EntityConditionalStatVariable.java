package slimeknights.tconstruct.library.json.variable.stat;

import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.library.json.variable.entity.EntityVariable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.LivingEntity;

/**
 * Gets a variable from the entity in the variable context
 *
 * @param entity   Entity variable getter
 * @param fallback Fallback if entity is null (happens when the tooltip is called serverside mainly)
 */
public record EntityConditionalStatVariable(EntityVariable entity, float fallback) implements ConditionalStatVariable {
    public static final RecordLoadable<EntityConditionalStatVariable> LOADER = RecordLoadable.create(
            EntityVariable.LOADER.directField("entity_type", EntityConditionalStatVariable::entity),
            FloatLoadable.ANY.requiredField("fallback", EntityConditionalStatVariable::fallback),
            EntityConditionalStatVariable::new);

    @Override
    public float getValue(IToolStackView tool, @Nullable LivingEntity entity) {
        if (entity != null) {
            return this.entity.getValue(entity);
        }
        return this.fallback;
    }

    @Override
    public IGenericLoader<? extends EntityConditionalStatVariable> getLoader() {
        return LOADER;
    }
}
