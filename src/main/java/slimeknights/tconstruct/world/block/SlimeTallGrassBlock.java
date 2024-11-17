package slimeknights.tconstruct.world.block;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.common.PlantType;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.world.TinkerWorld;

import javax.annotation.Nonnull;
import java.util.List;

public class SlimeTallGrassBlock extends PlantBlock implements IForgeShearable {

    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    @Getter
    private final FoliageType foliageType;

    public SlimeTallGrassBlock(Settings properties, FoliageType foliageType) {
        super(properties);
        this.foliageType = foliageType;
    }

    @Deprecated
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    /* Forge/MC callbacks */
    @Nonnull
    @Override
    public PlantType getPlantType(BlockView world, BlockPos pos) {
        return TinkerWorld.SLIME_PLANT_TYPE;
    }

    @Nonnull
    @Override
    public List<ItemStack> onSheared(@Nullable PlayerEntity player, ItemStack item, World world, BlockPos pos, int fortune) {
        return Lists.newArrayList(new ItemStack(this, 1));
    }

    @Override
    protected boolean canPlantOnTop(BlockState state, BlockView worldIn, BlockPos pos) {
        Block block = state.getBlock();
        return TinkerWorld.slimeDirt.contains(block) || TinkerWorld.vanillaSlimeGrass.contains(block) || TinkerWorld.earthSlimeGrass.contains(block) || TinkerWorld.skySlimeGrass.contains(block) || TinkerWorld.enderSlimeGrass.contains(block) || TinkerWorld.ichorSlimeGrass.contains(block);
    }

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (this.foliageType != FoliageType.ICHOR) {
            super.fillItemCategory(group, items);
        }
    }
}
