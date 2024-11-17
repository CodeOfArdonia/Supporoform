package slimeknights.mantle.registration;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Fluid properties' builder class, since the Forge one requires too many suppliers that we do not have access to yet
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class FluidBuilder<T extends FluidBuilder<T>> {
    protected Supplier<? extends FluidType> type;
    @Nullable
    protected Supplier<? extends Item> bucket;
    @Nullable
    protected Supplier<? extends FluidBlock> block;
    private int slopeFindDistance = 4;
    private int levelDecreasePerBlock = 1;
    private float explosionResistance = 1;
    private int tickRate = 5;

    /**
     * Creates a new builder instance
     */
    public static FluidBuilder<?> create(Supplier<? extends FluidType> type) {
        FluidBuilder<?> builder = new FluidBuilder<>();
        builder.type = type;
        return builder;
    }

    /**
     * Returns self casted to the given type
     */
    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    /**
     * Sets the supplier for the bucket
     */
    public T bucket(Supplier<? extends Item> value) {
        this.bucket = value;
        return this.self();
    }

    /**
     * Sets the supplier for the bucket
     */
    public T block(Supplier<? extends FluidBlock> value) {
        this.block = value;
        return this.self();
    }


    /* Basic properties */

    /**
     * Sets the slope find distance, only used in flowing fluids
     */
    public T slopeFindDistance(int value) {
        this.slopeFindDistance = value;
        return this.self();
    }

    /**
     * Sets how far the fluid can flow, only used in flowing fluids
     */
    public T levelDecreasePerBlock(int value) {
        this.levelDecreasePerBlock = value;
        return this.self();
    }

    /**
     * Sets the explosion resistance
     */
    public T explosionResistance(int value) {
        this.explosionResistance = value;
        return this.self();
    }

    /**
     * Sets the fluid tick rate
     */
    public T tickRate(int value) {
        this.tickRate = value;
        return this.self();
    }

    /**
     * Builds Forge fluid properties from this builder
     *
     * @param still   Still fluid supplier
     * @param flowing Flowing supplier
     * @return Forge fluid properties
     */
    public ForgeFlowingFluid.Properties build(Supplier<? extends FluidType> type, Supplier<? extends Fluid> still, Supplier<? extends Fluid> flowing) {
        return new ForgeFlowingFluid.Properties(type, still, flowing)
                .slopeFindDistance(this.slopeFindDistance)
                .levelDecreasePerBlock(this.levelDecreasePerBlock)
                .explosionResistance(this.explosionResistance)
                .tickRate(this.tickRate)
                .block(this.block)
                .bucket(this.bucket);
    }
}
