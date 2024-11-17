package slimeknights.mantle.registration.object;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import lombok.Getter;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Object containing registry entries for a fluid with a flowing form.
 *
 * @param <F> Fluid class
 */
@SuppressWarnings("WeakerAccess")
public class FlowingFluidObject<F extends FlowableFluid> extends FluidObject<F> {
    /**
     * Tag in the mod namespace, exact match
     */
    @Getter
    @NotNull
    private final TagKey<Fluid> localTag;
    private final Supplier<? extends F> flowing;
    @Nullable
    private final Supplier<? extends FluidBlock> block;

    /**
     * Main constructor
     */
    public FlowingFluidObject(Identifier id, String tagName, Supplier<? extends FluidType> type, Supplier<? extends F> still, Supplier<? extends F> flowing, @Nullable Supplier<? extends FluidBlock> block) {
        super(id, tagName, type, still);
        this.localTag = TagKey.of(RegistryKeys.FLUID, id);
        this.flowing = flowing;
        this.block = block;
    }

    /**
     * Gets the still form of this fluid. Alias for {@link #get()} for code readability.
     *
     * @return Still form
     * @see #get()
     */
    public F getStill() {
        return this.get();
    }

    /**
     * Gets the flowing form of this fluid
     *
     * @return flowing form
     */
    public F getFlowing() {
        return Objects.requireNonNull(this.flowing.get(), "Fluid object missing flowing fluid");
    }

    /**
     * Gets the block form of this fluid
     *
     * @return Block form
     */
    @Nullable
    public FluidBlock getBlock() {
        if (this.block == null) {
            return null;
        }
        return this.block.get();
    }

    @Override
    public FluidIngredient ingredient(int amount, boolean commonTag) {
        return FluidIngredient.of(commonTag ? this.getForgeTag() : this.getLocalTag(), amount);
    }
}
