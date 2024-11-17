package slimeknights.tconstruct.library.json.variable.stat;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.json.variable.VariableLoaderRegistry;
import slimeknights.tconstruct.library.json.variable.mining.MiningSpeedVariable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Variable for use in {@link slimeknights.tconstruct.library.modifiers.modules.behavior.ConditionalStatModule}.
 * All conditional stat variables automatically work as mining speed variables due to the superset parameter space.
 */
public interface ConditionalStatVariable extends IHaveLoader, MiningSpeedVariable {
    GenericLoaderRegistry<ConditionalStatVariable> LOADER = new VariableLoaderRegistry<>("Conditional Stat Variable", Constant::new);

    /**
     * Gets the value for the given content
     *
     * @param tool   Tool context
     * @param entity Entity context, will be null in tooltips
     * @return Value for the given context
     */
    float getValue(IToolStackView tool, @Nullable LivingEntity entity);

    @Override
    default float getValue(IToolStackView tool, @Nullable PlayerEvents.BreakSpeed event, @Nullable PlayerEntity player, @Nullable Direction sideHit) {
        return getValue(tool, player);
    }


    /**
     * Registers a variable with conditional stat and mining speed
     */
    static void register(Identifier name, IGenericLoader<? extends ConditionalStatVariable> loader) {
        LOADER.register(name, loader);
        MiningSpeedVariable.LOADER.register(name, loader);
    }


    /**
     * Constant value instance for this object
     */
    record Constant(float value) implements VariableLoaderRegistry.ConstantFloat, ConditionalStatVariable {
        public static final RecordLoadable<ConditionalStatVariable.Constant> LOADER = VariableLoaderRegistry.constantLoader(ConditionalStatVariable.Constant::new);

        @Override
        public float getValue(IToolStackView tool, @Nullable LivingEntity entity) {
            return value;
        }

        @Override
        public IGenericLoader<? extends ConditionalStatVariable> getLoader() {
            return LOADER;
        }
    }
}
