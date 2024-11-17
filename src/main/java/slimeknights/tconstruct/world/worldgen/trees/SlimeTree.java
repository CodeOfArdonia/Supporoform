package slimeknights.tconstruct.world.worldgen.trees;

import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.block.FoliageType;

public class SlimeTree extends SaplingGenerator {

    private final FoliageType foliageType;

    public SlimeTree(FoliageType foliageType) {
        this.foliageType = foliageType;
    }

    @Override
    protected RegistryEntry<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(Random random, boolean largeHive) {
        return (switch (this.foliageType) {
            case EARTH -> TinkerStructures.earthSlimeTree;
            case SKY -> TinkerStructures.skySlimeTree;
            case ENDER ->
                    random.nextFloat() < 0.85f ? TinkerStructures.enderSlimeTreeTall : TinkerStructures.enderSlimeTree;
            case BLOOD -> TinkerStructures.bloodSlimeFungus;
            case ICHOR -> TinkerStructures.ichorSlimeFungus;
        }).getHolder().orElseThrow();
    }
}
