package slimeknights.tconstruct.world.block;

import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import slimeknights.tconstruct.common.registration.GeodeItemObject;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;

public class BuddingCrystalBlock extends CrystalBlock {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final GeodeItemObject geode;

    public BuddingCrystalBlock(GeodeItemObject geode, SoundEvent chimeSound, Settings props) {
        super(chimeSound, props);
        this.geode = geode;
    }

    @Override
    public PistonBehavior getPistonPushReaction(BlockState pState) {
        return PistonBehavior.DESTROY;
    }

    @Override
    public void randomTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
        if (pRandom.nextInt(5) == 0) {
            Direction direction = DIRECTIONS[pRandom.nextInt(DIRECTIONS.length)];
            BlockPos blockpos = pPos.offset(direction);
            BlockState blockstate = pLevel.getBlockState(blockpos);
            Block block = null;
            if (BuddingAmethystBlock.canGrowIn(blockstate)) {
                block = this.geode.getBud(BudSize.SMALL);
            } else {
                for (BudSize size : BudSize.SIZES) {
                    if (blockstate.isOf(this.geode.getBud(size))) {
                        block = this.geode.getBud(size.getNext());
                        break;
                    }
                }
            }
            if (block != null) {
                BlockState state = block.getDefaultState().with(AmethystClusterBlock.FACING, direction).with(AmethystClusterBlock.WATERLOGGED, blockstate.getFluidState().getFluid() == Fluids.WATER);
                pLevel.setBlockState(blockpos, state);
            }
        }
    }
}
