package slimeknights.tconstruct.library.json.variable.mining;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.library.json.variable.block.BlockVariable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Uses a {@link BlockVariable} to fetch a value from the break speed event
 *
 * @param block    Block variable logic
 * @param fallback Fallback value if the event is null
 */
public record BlockMiningSpeedVariable(BlockVariable block, float fallback) implements MiningSpeedVariable {
    public static final RecordLoadable<BlockMiningSpeedVariable> LOADER = RecordLoadable.create(
            BlockVariable.LOADER.directField("block_type", BlockMiningSpeedVariable::block),
            FloatLoadable.ANY.requiredField("fallback", BlockMiningSpeedVariable::fallback),
            BlockMiningSpeedVariable::new);

    @Override
    public float getValue(IToolStackView tool, @Nullable PlayerEvents.BreakSpeed event, @Nullable PlayerEntity player, @Nullable Direction sideHit) {
        if (event != null) {
            return this.block.getValue(event.getState());
        }
        return this.fallback;
    }

    @Override
    public IGenericLoader<? extends MiningSpeedVariable> getLoader() {
        return LOADER;
    }
}
