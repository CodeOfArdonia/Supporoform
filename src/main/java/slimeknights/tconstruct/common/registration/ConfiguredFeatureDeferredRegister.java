package slimeknights.tconstruct.common.registration;

import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.GeodeCrackConfig;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.GeodeLayerConfig;
import net.minecraft.world.gen.feature.GeodeLayerThicknessConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.mantle.registration.deferred.DeferredRegisterWrapper;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;
import slimeknights.tconstruct.world.worldgen.trees.SupplierBlockStateProvider;

import java.util.Arrays;
import java.util.function.Supplier;

public class ConfiguredFeatureDeferredRegister extends DeferredRegisterWrapper<ConfiguredFeature<?, ?>> {
    public ConfiguredFeatureDeferredRegister(String modID) {
        super(Registries.CONFIGURED_FEATURE_REGISTRY, modID);
    }

    /**
     * Registers a configured feature with the register, for when the configuration needs to be built in a supplier
     *
     * @param name    Feature name
     * @param feature Parent feature
     * @param config  Configuration
     * @param <FC>    Config type
     * @param <F>     Feature type
     * @return Registered instance
     */
    public <FC extends FeatureConfig, F extends Feature<FC>> RegistryObject<ConfiguredFeature<FC, F>> registerSupplier(String name, Supplier<F> feature, Supplier<FC> config) {
        return this.register.register(name, () -> new ConfiguredFeature<>(feature.get(), config.get()));
    }

    /**
     * Registers a configured feature with the register, for when the configuration can be safely statically built
     *
     * @param name    Feature name
     * @param feature Parent feature
     * @param config  Configuration
     * @param <FC>    Config type
     * @param <F>     Feature type
     * @return Registered instance
     */
    public <FC extends FeatureConfig, F extends Feature<FC>> RegistryObject<ConfiguredFeature<FC, F>> registerStatic(String name, Supplier<F> feature, FC config) {
        return this.register.register(name, () -> new ConfiguredFeature<>(feature.get(), config));
    }

    /**
     * Registers a configured geode
     */
    public RegistryObject<ConfiguredFeature<GeodeFeatureConfig, Feature<GeodeFeatureConfig>>> registerGeode(String name, GeodeItemObject geode,
                                                                                                            BlockStateProvider middleLayer, BlockStateProvider outerLayer, GeodeLayerThicknessConfig layerSettings, GeodeCrackConfig crackSettings,
                                                                                                            IntProvider outerWall, IntProvider distributionPoints, IntProvider pointOffset, int genOffset, int invalidBlocks) {
        return this.registerSupplier(name, () -> Feature.GEODE, () -> new GeodeFeatureConfig(
                new GeodeLayerConfig(BlockStateProvider.of(Blocks.AIR),
                        BlockStateProvider.of(geode.getBlock()),
                        SupplierBlockStateProvider.ofBlock(geode::getBudding),
                        middleLayer, outerLayer,
                        Arrays.stream(BudSize.values()).map(type -> geode.getBud(type).getDefaultState()).toList(),
                        BlockTags.FEATURES_CANNOT_REPLACE, BlockTags.GEODE_INVALID_BLOCKS),
                layerSettings, crackSettings, 0.335, 0.083, true, outerWall, distributionPoints, pointOffset, -genOffset, genOffset, 0.05D, invalidBlocks));
    }
}
