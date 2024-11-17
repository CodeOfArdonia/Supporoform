package slimeknights.tconstruct.library.json.variable.entity;

import net.minecraft.entity.LivingEntity;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.library.json.variable.ConditionalVariable;

/**
 * Gets one of two entity properties based on the condition
 */
public record ConditionalEntityVariable(IJsonPredicate<LivingEntity> condition, EntityVariable ifTrue,
                                        EntityVariable ifFalse)
        implements EntityVariable, ConditionalVariable<IJsonPredicate<LivingEntity>, EntityVariable> {
    public static final IGenericLoader<ConditionalEntityVariable> LOADER = ConditionalVariable.loadable(LivingEntityPredicate.LOADER, EntityVariable.LOADER, ConditionalEntityVariable::new);

    public ConditionalEntityVariable(IJsonPredicate<LivingEntity> condition, float ifTrue, float ifFalse) {
        this(condition, new Constant(ifTrue), new Constant(ifFalse));
    }

    @Override
    public float getValue(LivingEntity entity) {
        return this.condition.matches(entity) ? this.ifTrue.getValue(entity) : this.ifFalse.getValue(entity);
    }

    @Override
    public IGenericLoader<? extends EntityVariable> getLoader() {
        return LOADER;
    }

}
