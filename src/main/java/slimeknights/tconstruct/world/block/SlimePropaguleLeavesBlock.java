package slimeknights.tconstruct.world.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import slimeknights.tconstruct.world.TinkerWorld;

import static net.minecraft.block.PropaguleBlock.AGE;
import static net.minecraft.state.property.Properties.HANGING;

/**
 * Recreation of {@link net.minecraft.block.MangroveLeavesBlock} with slimy leaves behavior
 */
public class SlimePropaguleLeavesBlock extends SlimeLeavesBlock implements Fertilizable {
    public SlimePropaguleLeavesBlock(Settings properties, FoliageType foliageType) {
        super(properties, foliageType);
    }

    @Override
    public boolean isValidBonemealTarget(BlockView level, BlockPos pos, BlockState state, boolean isClient) {
        return level.getBlockState(pos.down()).isAir();
    }

    @Override
    public boolean canGrow(World level, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld level, Random random, BlockPos pos, BlockState state) {
        level.setBlockState(pos.down(), TinkerWorld.slimeSapling.get(FoliageType.ENDER).getDefaultState().with(HANGING, Boolean.TRUE).with(AGE, 0), 2);
    }
}
