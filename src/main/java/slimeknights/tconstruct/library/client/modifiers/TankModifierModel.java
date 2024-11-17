package slimeknights.tconstruct.library.client.modifiers;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.fluid.Fluid;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.function.Function;

/**
 * Model for tank modifiers, also displays the fluid
 */
public class TankModifierModel extends FluidModifierModel {
    /**
     * Constant unbaked model instance, as they are all the same
     */
    public static final IUnbakedModifierModel UNBAKED_INSTANCE = new Unbaked(ToolTankHelper.TANK_HELPER);

    public TankModifierModel(ToolTankHelper helper,
                             @Nullable SpriteIdentifier smallTexture, @Nullable SpriteIdentifier largeTexture,
                             @Nullable SpriteIdentifier smallPartial, @Nullable SpriteIdentifier largePartial,
                             @Nullable SpriteIdentifier smallFull, @Nullable SpriteIdentifier largeFull) {
        super(helper, smallTexture, largeTexture, new SpriteIdentifier[]{smallPartial, largePartial, smallFull, largeFull});
    }

    @Nullable
    @Override
    public Object getCacheKey(IToolStackView tool, ModifierEntry entry) {
        FluidStack fluid = this.helper.getFluid(tool);
        if (!fluid.isEmpty()) {
            // cache by modifier, fluid, and not being full
            return new TankModifierCacheKey(entry.getModifier(), fluid.getFluid(), fluid.getAmount() < this.helper.getCapacity(tool));
        }
        return entry != ModifierEntry.EMPTY ? entry.getId() : null;
    }

    @Override
    @Nullable
    protected SpriteIdentifier getTemplate(IToolStackView tool, ModifierEntry entry, FluidStack fluid, boolean isLarge) {
        boolean isFull = fluid.getAmount() == this.helper.getCapacity(tool);
        return this.fluidTextures[(isFull ? 2 : 0) | (isLarge ? 1 : 0)];
    }

    /**
     * Cache key for the model
     */
    private record TankModifierCacheKey(Modifier modifier, Fluid fluid, boolean isPartial) {
    }

    public record Unbaked(ToolTankHelper helper) implements IUnbakedModifierModel {
        @Nullable
        @Override
        public IBakedModifierModel forTool(Function<String, SpriteIdentifier> smallGetter, Function<String, SpriteIdentifier> largeGetter) {
            SpriteIdentifier smallTexture = smallGetter.apply("");
            SpriteIdentifier largeTexture = largeGetter.apply("");
            SpriteIdentifier smallPartial = smallGetter.apply("_partial");
            SpriteIdentifier largePartial = largeGetter.apply("_partial");
            SpriteIdentifier smallFull = smallGetter.apply("_full");
            SpriteIdentifier largeFull = largeGetter.apply("_full");
            if (smallTexture != null || largeTexture != null || smallPartial != null || largePartial != null || smallFull != null || largeFull != null) {
                return new TankModifierModel(this.helper, smallTexture, largeTexture, smallPartial, largePartial, smallFull, largeFull);
            }
            return null;
        }
    }
}
