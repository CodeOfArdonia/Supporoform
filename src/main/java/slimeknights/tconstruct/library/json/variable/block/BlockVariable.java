package slimeknights.tconstruct.library.json.variable.block;

import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.json.variable.ToFloatFunction;
import slimeknights.tconstruct.library.json.variable.VariableLoaderRegistry;

import static slimeknights.mantle.data.registry.GenericLoaderRegistry.SingletonLoader.singleton;

import net.minecraft.block.BlockState;

/**
 * Variable that fetches a property from a block state
 */
public interface BlockVariable extends IHaveLoader {
    GenericLoaderRegistry<BlockVariable> LOADER = new VariableLoaderRegistry<>("Block Variable", Constant::new);

    /**
     * Gets a value from the given block state
     */
    float getValue(BlockState state);

    @Override
    IGenericLoader<? extends BlockVariable> getLoader();

    /* Singletons */

    /**
     * Creates a new singleton variable getter
     */
    static BlockVariable simple(ToFloatFunction<BlockState> getter) {
        return singleton(loader -> new BlockVariable() {
            @Override
            public float getValue(BlockState state) {
                return getter.apply(state);
            }

            @Override
            public IGenericLoader<? extends BlockVariable> getLoader() {
                return loader;
            }
        });
    }

    /**
     * Gets the block's blast resistance
     */
    BlockVariable BLAST_RESISTANCE = simple(state -> state.getBlock().getBlastResistance());
    /**
     * Gets the block's hardness
     */
    BlockVariable HARDNESS = simple(state -> state.getBlock().getHardness());


    /**
     * Constant value instance for this object
     */
    record Constant(float value) implements VariableLoaderRegistry.ConstantFloat, BlockVariable {
        public static final RecordLoadable<Constant> LOADER = VariableLoaderRegistry.constantLoader(Constant::new);

        @Override
        public float getValue(BlockState state) {
            return value;
        }

        @Override
        public IGenericLoader<? extends BlockVariable> getLoader() {
            return LOADER;
        }
    }
}
