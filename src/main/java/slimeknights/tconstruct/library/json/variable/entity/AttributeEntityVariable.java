package slimeknights.tconstruct.library.json.variable.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;

/**
 * Variable that fetches an attribute value
 */
public record AttributeEntityVariable(EntityAttribute attribute) implements EntityVariable {
    public static final RecordLoadable<AttributeEntityVariable> LOADER = RecordLoadable.create(Loadables.ATTRIBUTE.requiredField("attribute", AttributeEntityVariable::attribute), AttributeEntityVariable::new);

    @Override
    public float getValue(LivingEntity entity) {
        return (float) entity.getAttributeValue(this.attribute);
    }

    @Override
    public IGenericLoader<? extends EntityVariable> getLoader() {
        return LOADER;
    }
}
