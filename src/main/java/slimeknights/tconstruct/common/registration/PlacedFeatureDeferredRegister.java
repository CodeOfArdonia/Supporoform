package slimeknights.tconstruct.common.registration;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.mantle.registration.deferred.DeferredRegisterWrapper;

import java.util.Arrays;
import java.util.List;

public class PlacedFeatureDeferredRegister extends DeferredRegisterWrapper<PlacedFeature> {
    public PlacedFeatureDeferredRegister(String modID) {
        super(Registry.PLACED_FEATURE_REGISTRY, modID);
    }

    /**
     * Registers a placed feature
     *
     * @param name      Feature name
     * @param feature   Configured feature base
     * @param placement Placements
     * @return Registry object
     */
    public RegistryObject<PlacedFeature> register(String name, RegistryObject<? extends ConfiguredFeature<?, ?>> feature, List<PlacementModifier> placement) {
        return this.register.register(name, () -> new PlacedFeature(RegistryEntry.hackyErase(feature.getHolder().orElseThrow(() -> new IllegalStateException("Feature does not have a holder"))), List.copyOf(placement)));
    }

    /**
     * Registers a placed feature
     *
     * @param name      Feature name
     * @param feature   Configured feature base
     * @param placement Placements
     * @return Registry object
     */
    public RegistryObject<PlacedFeature> register(String name, RegistryObject<? extends ConfiguredFeature<?, ?>> feature, PlacementModifier... placement) {
        return this.register(name, feature, Arrays.asList(placement));
    }

    /**
     * Registers a geode feature
     */
    public RegistryObject<PlacedFeature> registerGeode(String name, RegistryObject<ConfiguredFeature<GeodeFeatureConfig, Feature<GeodeFeatureConfig>>> geode, RarityFilterPlacementModifier rarity, HeightRangePlacementModifier height) {
        return this.register(name, geode, rarity, SquarePlacementModifier.of(), height, BiomePlacementModifier.of());
    }
}
