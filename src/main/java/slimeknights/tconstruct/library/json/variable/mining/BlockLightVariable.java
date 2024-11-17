package slimeknights.tconstruct.library.json.variable.mining;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Gets the targeted block light level. Will use the targeted position if possible, otherwise the players position
 *
 * @param lightLayer Block light layer to use
 * @param fallback   Fallback value if missing event and player
 */
public record BlockLightVariable(LightType lightLayer, float fallback) implements MiningSpeedVariable {
    public static final RecordLoadable<BlockLightVariable> LOADER = RecordLoadable.create(
            TinkerLoadables.LIGHT_LAYER.requiredField("light_layer", BlockLightVariable::lightLayer),
            FloatLoadable.ANY.requiredField("fallback", BlockLightVariable::fallback),
            BlockLightVariable::new);

    @Override
    public float getValue(IToolStackView tool, @Nullable PlayerEvents.BreakSpeed event, @Nullable PlayerEntity player, @Nullable Direction sideHit) {
        if (player != null) {
            // use block position if possible player position otherwise
            return player.getWorld().getLightLevel(this.lightLayer, event != null && sideHit != null ? event.getPos().add(sideHit.getVector()) : player.getBlockPos());
        }
        return this.fallback;
    }

    @Override
    public IGenericLoader<? extends MiningSpeedVariable> getLoader() {
        return LOADER;
    }
}
