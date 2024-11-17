package slimeknights.tconstruct.world.worldgen.trees.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.gen.feature.HugeFungusFeatureConfig;

/**
 * Extension of huge fungus config that replaces the ground state with a ground tag
 */
public class SlimeFungusConfig extends HugeFungusFeatureConfig {
    public static final Codec<HugeFungusFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.unprefixedCodec(Registry.BLOCK_REGISTRY).fieldOf("valid_base").forGetter(
                    config -> config instanceof SlimeFungusConfig ? ((SlimeFungusConfig) config).getGroundTag() : BlockTags.NYLIUM),
            BlockState.CODEC.fieldOf("stem_state").forGetter(config -> config.stemState),
            BlockState.CODEC.fieldOf("hat_state").forGetter(config -> config.hatState),
            BlockState.CODEC.fieldOf("decor_state").forGetter(config -> config.decorState),
            Codec.BOOL.fieldOf("planted").orElse(false).forGetter(config -> config.planted)
    ).apply(instance, SlimeFungusConfig::new));

    @Getter
    private final TagKey<Block> groundTag;

    public SlimeFungusConfig(TagKey<Block> groundTag, BlockState stem, BlockState hat, BlockState decor, boolean planted) {
        super(Blocks.AIR.getDefaultState(), stem, hat, decor, planted);
        this.groundTag = groundTag;
    }
}
