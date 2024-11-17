package slimeknights.tconstruct.library.json.variable.block;

import net.minecraft.block.BlockState;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.library.json.variable.ConditionalVariable;

/**
 * Gets one of two block properties based on the condition
 */
public record ConditionalBlockVariable(IJsonPredicate<BlockState> condition, BlockVariable ifTrue,
                                       BlockVariable ifFalse) implements BlockVariable, ConditionalVariable<IJsonPredicate<BlockState>, BlockVariable> {
    public static final IGenericLoader<ConditionalBlockVariable> LOADER = ConditionalVariable.loadable(BlockPredicate.LOADER, BlockVariable.LOADER, ConditionalBlockVariable::new);

    @Override
    public float getValue(BlockState state) {
        return this.condition.matches(state) ? this.ifTrue.getValue(state) : this.ifFalse.getValue(state);
    }

    @Override
    public IGenericLoader<? extends BlockVariable> getLoader() {
        return LOADER;
    }
}
