package slimeknights.tconstruct.smeltery.block.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity.ITankBlock;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class SearedTankBlock extends SearedBlock implements ITankBlock, BlockEntityProvider {
    @Getter
    private final int capacity;
    private final PistonBehavior pushReaction;

    public SearedTankBlock(Settings properties, int capacity, PistonBehavior pushReaction) {
        super(properties, true);
        this.capacity = capacity;
        this.pushReaction = pushReaction;
    }

    public SearedTankBlock(Settings properties, int capacity) {
        this(properties, capacity, PistonBehavior.BLOCK);
    }

    @Override
    public PistonBehavior getPistonPushReaction(BlockState pState) {
        return this.pushReaction;
    }

    @Deprecated
    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView worldIn, BlockPos pos) {
        return 1.0F;
    }

    @Override
    @Nullable
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new TankBlockEntity(pPos, pState, this);
    }

    @Deprecated
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (FluidTransferHelper.interactWithTank(world, pos, player, hand, hit)) {
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }



    @Override
    public int getLightEmission(BlockState state, BlockView world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof TankBlockEntity) {
            FluidStack fluid = ((TankBlockEntity) te).getTank().getFluid();
            return fluid.getFluid().getFluidType().getLightLevel(fluid);
        }
        return super.getLightEmission(state, world, pos);
    }

    @Override
    public void onPlaced(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            BlockEntityHelper.get(TankBlockEntity.class, worldIn, pos).ifPresent(te -> te.updateTank(nbt.getCompound(NBTTags.TANK)));
        }
        super.onPlaced(worldIn, pos, state, placer, stack);
    }

    @Deprecated
    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Deprecated
    @Override
    public int getComparatorOutput(BlockState blockState, World worldIn, BlockPos pos) {
        return ITankBlockEntity.getComparatorInputOverride(worldIn, pos);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        BlockEntityHelper.get(TankBlockEntity.class, world, pos).ifPresent(te -> te.setTankTag(stack));
        return stack;
    }

    @Getter
    @AllArgsConstructor
    public enum TankType implements StringIdentifiable {
        FUEL_TANK(TankBlockEntity.DEFAULT_CAPACITY),
        FUEL_GAUGE(TankBlockEntity.DEFAULT_CAPACITY),
        INGOT_TANK(FluidValues.INGOT * 48),
        INGOT_GAUGE(FluidValues.INGOT * 48);

        private final int capacity;

        @Override
        public String asString() {
            return this.toString().toLowerCase(Locale.US);
        }
    }
}
