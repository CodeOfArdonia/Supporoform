package slimeknights.mantle.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.registration.FluidBuilder;

import java.util.function.Supplier;

/**
 * Fluid with a bucket form, but no block form (hence no flowing)
 */
@AllArgsConstructor
public class UnplaceableFluid extends Fluid {
    /**
     * Forge fluid attributes builder
     */
    private final Supplier<? extends FluidType> type;
    /**
     * Bucket form of the liquid, use a supplier to air if no bucket form
     */
    @Nullable
    private final Supplier<? extends Item> bucket;
    @Getter
    private final float explosionResistance;
    private final int tickRate;

    public UnplaceableFluid(FluidBuilder<?> builder) {
        this.type = builder.getType();
        this.bucket = builder.getBucket();
        this.explosionResistance = builder.getExplosionResistance();
        this.tickRate = builder.getTickRate();
    }

    @SuppressWarnings("unused")  // API
    public UnplaceableFluid(Supplier<? extends FluidType> type, @Nullable Supplier<? extends Item> bucket) {
        this(type, bucket, 100, 5);
    }


    @Override
    public FluidType getFluidType() {
        return this.type.get();
    }

    @Override
    public Item getBucketItem() {
        if (this.bucket == null) {
            return Items.AIR;
        }
        return this.bucket.get();
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction side) {
        return false;
    }

    @Override
    public int getTickRate(WorldView world) {
        return this.tickRate;
    }

    @Override
    protected BlockState toBlockState(FluidState state) {
        return Blocks.AIR.getDefaultState();
    }


    /* Required methods */

    @Override
    protected Vec3d getVelocity(BlockView world, BlockPos pos, FluidState state) {
        return Vec3d.ZERO;
    }

    @Override
    public boolean isStill(FluidState state) {
        return true;
    }

    @Override
    public float getHeight(FluidState state) {
        return 1;
    }

    @Override
    public float getHeight(FluidState state, BlockView world, BlockPos pos) {
        return 1;
    }

    @Override
    public int getLevel(FluidState state) {
        return 0;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockView world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }
}
