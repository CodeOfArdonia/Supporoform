package slimeknights.tconstruct.library.json.variable.mining;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.json.variable.VariableLoaderRegistry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Variable used for {@link slimeknights.tconstruct.library.modifiers.modules.mining.ConditionalMiningSpeedModule}
 */
public interface MiningSpeedVariable extends IHaveLoader {
    GenericLoaderRegistry<MiningSpeedVariable> LOADER = new VariableLoaderRegistry<>("Mining Speed Variable", Constant::new);

    /**
     * Gets the value of this variable
     *
     * @param tool    Tool instance
     * @param event   Break speed event, may be null on tooltips
     * @param player  Player instance, may be defined when event is null, but still may be null on tooltips
     * @param sideHit Block side hit, may be null on tooltips
     * @return Value of this variable, using a fallback if appropiate
     */
    float getValue(IToolStackView tool, @Nullable PlayerEvents.BreakSpeed event, @Nullable PlayerEntity player, @Nullable Direction sideHit);

    /**
     * Constant value instance for this object
     */
    record Constant(float value) implements VariableLoaderRegistry.ConstantFloat, MiningSpeedVariable {
        public static final RecordLoadable<Constant> LOADER = VariableLoaderRegistry.constantLoader(Constant::new);

        @Override
        public float getValue(IToolStackView tool, @Nullable PlayerEvents.BreakSpeed event, @Nullable PlayerEntity player, @Nullable Direction sideHit) {
            return this.value;
        }

        @Override
        public IGenericLoader<? extends MiningSpeedVariable> getLoader() {
            return LOADER;
        }
    }
}
