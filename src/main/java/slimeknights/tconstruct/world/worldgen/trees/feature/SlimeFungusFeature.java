package slimeknights.tconstruct.world.worldgen.trees.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.HugeFungusFeature;
import net.minecraft.world.gen.feature.HugeFungusFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import slimeknights.tconstruct.world.worldgen.trees.config.SlimeFungusConfig;

public class SlimeFungusFeature extends HugeFungusFeature {
    public SlimeFungusFeature(Codec<HugeFungusFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<HugeFungusFeatureConfig> context) {
        if (!(context.getConfig() instanceof SlimeFungusConfig config)) {
            return super.generate(context);
        }
        // must be on the right ground
        StructureWorldAccess level = context.getWorld();
        BlockPos pos = context.getOrigin();
        if (!level.getBlockState(pos.down()).isIn(config.getGroundTag())) {
            return false;
        }
        // ensure not too tall
        Random random = context.getRandom();
        int height = MathHelper.nextInt(random, 4, 13);
        if (random.nextInt(12) == 0) {
            height *= 2;
        }
        if (!config.planted && pos.getY() + height + 1 >= context.getGenerator().getWorldHeight()) {
            return false;
        }
        // actual generation
        boolean flag = !config.planted && random.nextFloat() < 0.06F;
        level.setBlockState(pos, Blocks.AIR.getDefaultState(), 4);
        this.generateStem(level, random, config, pos, height, flag);
        this.generateHat(level, random, config, pos, height, flag);
        return true;
    }
}
