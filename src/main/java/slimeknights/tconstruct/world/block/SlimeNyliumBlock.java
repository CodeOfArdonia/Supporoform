package slimeknights.tconstruct.world.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.level.lighting.LayerLightEngine;
import slimeknights.tconstruct.common.TinkerTags;

/**
 * Slimy variant of nylium, mostly changes the way it bonemeals
 */
public class SlimeNyliumBlock extends Block implements Fertilizable {
    private final FoliageType foliageType;

    public SlimeNyliumBlock(Settings properties, FoliageType foliageType) {
        super(properties);
        this.foliageType = foliageType;
    }

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (this.foliageType != FoliageType.ICHOR) {
            super.fillItemCategory(group, items);
        }
    }

    private static boolean isDarkEnough(BlockState state, WorldView reader, BlockPos pos) {
        BlockPos blockpos = pos.up();
        BlockState blockstate = reader.getBlockState(blockpos);
        int i = LayerLightEngine.getLightBlockInto(reader, state, pos, blockstate, blockpos, Direction.UP, blockstate.getOpacity(reader, blockpos));
        return i < reader.getMaxLightLevel();
    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        if (!isDarkEnough(state, worldIn, pos)) {
            worldIn.setBlockState(pos, SlimeGrassBlock.getDirtState(state));
        }
    }

    @Override
    public boolean isValidBonemealTarget(BlockView worldIn, BlockPos pos, BlockState state, boolean isClient) {
        return worldIn.getBlockState(pos.up()).isAir();
    }

    @Override
    public boolean canGrow(World worldIn, Random rand, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random rand, BlockPos pos, BlockState state) {
        SlimeGrassBlock.growGrass(world, rand, pos, TinkerTags.Blocks.SLIMY_NYLIUM, this.foliageType, true, true);
    }
}
