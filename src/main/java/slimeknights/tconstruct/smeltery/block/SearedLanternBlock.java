package slimeknights.tconstruct.smeltery.block;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.LanternBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity.ITankBlock;

public class SearedLanternBlock extends LanternBlock implements ITankBlock, BlockEntityProvider {
    @Getter
    private final int capacity;

    public SearedLanternBlock(Settings properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LanternBlockEntity(pos, state, this);
    }

    @Override
    public int getLightEmission(BlockState state, BlockView world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof TankBlockEntity tank) {
            FluidStack fluid = tank.getTank().getFluid();
            return fluid.getFluid().getFluidType().getLightLevel(fluid);
        }
        return 0;
    }

    @Override
    public void onPlaced(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            BlockEntityHelper.get(TankBlockEntity.class, worldIn, pos).ifPresent(te -> te.updateTank(nbt.getCompound(NBTTags.TANK)));
        }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
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
}
