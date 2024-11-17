package slimeknights.tconstruct.world.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.FungusBlock;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.HugeFungusFeatureConfig;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.function.Supplier;

/**
 * Update of fungus that grows on slime soil instead
 */
public class SlimeFungusBlock extends FungusBlock {
    public SlimeFungusBlock(Settings properties, Supplier<RegistryEntry<ConfiguredFeature<HugeFungusFeatureConfig, ?>>> fungusFeature) {
        super(properties, fungusFeature);
    }

    @Override
    protected boolean canPlantOnTop(BlockState state, BlockView worldIn, BlockPos pos) {
        return state.isIn(TinkerTags.Blocks.SLIMY_SOIL);
    }

    @Override
    public boolean isValidBonemealTarget(BlockView worldIn, BlockPos pos, BlockState state, boolean isClient) {
        return worldIn.getBlockState(pos.down()).isIn(TinkerTags.Blocks.SLIMY_SOIL);
    }
}
