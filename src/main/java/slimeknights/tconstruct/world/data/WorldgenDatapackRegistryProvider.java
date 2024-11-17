package slimeknights.tconstruct.world.data;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Pool;
import net.minecraft.world.StructureSpawns;
import net.minecraft.world.StructureSpawns.BoundingBox;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;
import net.minecraft.world.gen.GenerationStep.Feature;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.feature.TreeConfiguredFeatures;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.Config;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers.AddFeaturesBiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers.AddSpawnsBiomeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.holdersets.AndHolderSet;
import net.minecraftforge.registries.holdersets.NotHolderSet;
import net.minecraftforge.registries.holdersets.OrHolderSet;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.block.FoliageType;
import slimeknights.tconstruct.world.worldgen.islands.IslandStructure;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.minecraft.registry.entry.RegistryEntryList.of;
import static slimeknights.tconstruct.TConstruct.getResource;
import static slimeknights.tconstruct.world.TinkerStructures.*;

/**
 * Provider for all our worldgen datapack registry stuff
 */
@SuppressWarnings("SameParameterValue")
@RequiredArgsConstructor
public class WorldgenDatapackRegistryProvider implements DataProvider {
    private final DataGenerator generator;
    private final ExistingFileHelper existingFileHelper;
    private final DynamicRegistryManager registryAccess = DynamicRegistryManager.builtinCopy();
    private final RegistryOps<JsonElement> registryOps = RegistryOps.of(JsonOps.INSTANCE, this.registryAccess);

    @Override
    public void run(DataWriter cache) throws IOException {
        Map<RegistryKey<Structure>, Structure> structures = new LinkedHashMap<>();
        // earthslime island
        structures.put(earthSlimeIsland, IslandStructure.seaBuilder()
                .addDefaultTemplates(getResource("islands/earth/"))
                .addTree(reference(earthSlimeIslandTree), 1)
                .addSlimyGrass(FoliageType.EARTH)
                .build(new Config(this.tag(TinkerTags.Biomes.EARTHSLIME_ISLANDS), monsterOverride(EntityType.SLIME, 4, 4), Feature.SURFACE_STRUCTURES, StructureTerrainAdaptation.NONE)));
        // skyslime island
        structures.put(skySlimeIsland, IslandStructure.skyBuilder()
                .addDefaultTemplates(getResource("islands/sky/"))
                .addTree(reference(skySlimeIslandTree), 1)
                .addSlimyGrass(FoliageType.SKY)
                .vines(TinkerWorld.skySlimeVine.get())
                .build(new Config(this.tag(TinkerTags.Biomes.SKYSLIME_ISLANDS), monsterOverride(TinkerWorld.skySlimeEntity.get(), 3, 4), Feature.SURFACE_STRUCTURES, StructureTerrainAdaptation.NONE)));
        // clay island
        structures.put(clayIsland, IslandStructure.skyBuilder().addDefaultTemplates(getResource("islands/dirt/"))
                .addTree(this.reference(TreeConfiguredFeatures.OAK), 4)
                .addTree(this.reference(TreeConfiguredFeatures.BIRCH), 3)
                .addTree(this.reference(TreeConfiguredFeatures.SPRUCE), 2)
                .addTree(this.reference(TreeConfiguredFeatures.ACACIA), 1)
                .addTree(this.reference(TreeConfiguredFeatures.JUNGLE_TREE_NO_VINE), 1)
                .addGrass(Blocks.GRASS, 7)
                .addGrass(Blocks.FERN, 1)
                .build(new Config(this.tag(TinkerTags.Biomes.CLAY_ISLANDS), monsterOverride(TinkerWorld.terracubeEntity.get(), 2, 4), Feature.SURFACE_STRUCTURES, StructureTerrainAdaptation.NONE)));
        // blood island
        structures.put(bloodIsland, IslandStructure.seaBuilder().addDefaultTemplates(getResource("islands/blood/"))
                .addTree(reference(bloodSlimeIslandFungus), 1)
                .addSlimyGrass(FoliageType.BLOOD)
                .build(new Config(this.tag(TinkerTags.Biomes.BLOOD_ISLANDS), monsterOverride(EntityType.MAGMA_CUBE, 4, 6), Feature.UNDERGROUND_DECORATION, StructureTerrainAdaptation.NONE)));
        // enderslime
        structures.put(endSlimeIsland, IslandStructure.skyBuilder().addDefaultTemplates(getResource("islands/ender/"))
                .addTree(reference(enderSlimeTree), 3)
                .addTree(reference(enderSlimeTreeTall), 17)
                .addSlimyGrass(FoliageType.ENDER)
                .vines(TinkerWorld.enderSlimeVine.get())
                .build(new Config(this.tag(TinkerTags.Biomes.ENDERSLIME_ISLANDS), monsterOverride(TinkerWorld.enderSlimeEntity.get(), 4, 4), Feature.SURFACE_STRUCTURES, StructureTerrainAdaptation.NONE)));

        // structure sets
        Map<String, StructureSet> structureSets = new LinkedHashMap<>();
        structureSets.put("overworld_ocean_island", this.structureSet(new RandomSpreadStructurePlacement(35, 25, SpreadType.LINEAR, 25988585), this.entry(earthSlimeIsland, 1)));
        structureSets.put("overworld_sky_island", this.structureSet(new RandomSpreadStructurePlacement(40, 15, SpreadType.LINEAR, 14357800), this.entry(skySlimeIsland, 4), this.entry(clayIsland, 1)));
        structureSets.put("nether_ocean_island", this.structureSet(new RandomSpreadStructurePlacement(15, 10, SpreadType.LINEAR, 65245622), this.entry(bloodIsland, 1)));
        structureSets.put("end_sky_island", this.structureSet(new RandomSpreadStructurePlacement(25, 12, SpreadType.LINEAR, 368963602), this.entry(endSlimeIsland, 1)));

        // biome modifiers
        Map<String, BiomeModifier> biomeModifiers = new LinkedHashMap<>();
        RegistryEntryList<Biome> overworld = this.tag(BiomeTags.IS_OVERWORLD);
        RegistryEntryList<Biome> nether = this.tag(BiomeTags.IS_NETHER);
        RegistryEntryList<Biome> end = this.tag(BiomeTags.IS_END);

        biomeModifiers.put("cobalt_ore", new AddFeaturesBiomeModifier(nether, of(reference(TinkerWorld.placedSmallCobaltOre), reference(TinkerWorld.placedLargeCobaltOre)), Feature.UNDERGROUND_DECORATION));
        // geodes
        biomeModifiers.put("earth_geode", new AddFeaturesBiomeModifier(overworld, of(reference(TinkerWorld.placedEarthGeode)), Feature.LOCAL_MODIFICATIONS));
        biomeModifiers.put("sky_geode", new AddFeaturesBiomeModifier(this.and(overworld, this.not(Registry.BIOME_REGISTRY, this.or(this.tag(BiomeTags.IS_OCEAN), this.tag(BiomeTags.IS_DEEP_OCEAN), this.tag(BiomeTags.IS_BEACH), this.tag(BiomeTags.IS_RIVER)))), of(reference(TinkerWorld.placedSkyGeode)), Feature.LOCAL_MODIFICATIONS));
        biomeModifiers.put("ichor_geode", new AddFeaturesBiomeModifier(nether, of(reference(TinkerWorld.placedIchorGeode)), Feature.LOCAL_MODIFICATIONS));
        biomeModifiers.put("ender_geode", new AddFeaturesBiomeModifier(this.and(end, this.not(Registry.BIOME_REGISTRY, of(this.reference(BiomeKeys.THE_END)))), of(reference(TinkerWorld.placedEnderGeode)), Feature.LOCAL_MODIFICATIONS));
        // spawns
        biomeModifiers.put("spawn_overworld_slime", new AddSpawnsBiomeModifier(overworld, List.of(new SpawnEntry(TinkerWorld.skySlimeEntity.get(), 100, 2, 4))));
        biomeModifiers.put("spawn_end_slime", new AddSpawnsBiomeModifier(end, List.of(new SpawnEntry(TinkerWorld.enderSlimeEntity.get(), 10, 2, 4))));

        // run final loading
        this.registryName(Registry.STRUCTURE_SET_REGISTRY, structureSets).run(cache);
        this.registryKey(Registry.STRUCTURE_REGISTRY, structures).run(cache);
        this.registryName(ForgeRegistries.Keys.BIOME_MODIFIERS, biomeModifiers).run(cache);
    }

    @Override
    public String getName() {
        return "Tinkers' Construct Worldgen Datapack Registries";
    }


    /* Registry helpers */

    /**
     * Creates a reference to the given registry object
     */
    private <T> RegistryEntry<T> reference(RegistryKey<T> key) {
        RegistryKey<Registry<T>> registry = RegistryKey.ofRegistry(key.getRegistry());
        return this.registryAccess.get(registry).getOrCreateHolderOrThrow(Objects.requireNonNull(key));
    }

    /**
     * Creates a reference to the given registry object
     */
    private <T> RegistryEntry<T> reference(RegistryEntry<T> object) {
        return this.reference(object.getKey().orElseThrow());
    }

    /**
     * Creates a reference to the given registry object
     */
    private <T> RegistryEntry<T> reference(RegistryObject<T> object) {
        return reference(Objects.requireNonNull(object.getKey()));
    }


    /* Holder sets */

    /**
     * Creates a holder set tag for the given registry
     */
    private <T> RegistryEntryList<T> tag(TagKey<T> key) {
        return this.registryAccess.get(key.registry()).getOrCreateEntryList(key);
    }

    /**
     * Ands the holder sets together
     */
    @SafeVarargs
    private <T> AndHolderSet<T> and(RegistryEntryList<T>... sets) {
        return new AndHolderSet<>(List.of(sets));
    }

    /**
     * Ors the holder sets together
     */
    @SafeVarargs
    private <T> OrHolderSet<T> or(RegistryEntryList<T>... sets) {
        return new OrHolderSet<>(List.of(sets));
    }

    private <T> NotHolderSet<T> not(RegistryKey<Registry<T>> key, RegistryEntryList<T> set) {
        return new NotHolderSet<>(this.registryAccess.get(key), set);
    }


    /* Datapack helpers */

    /**
     * Creates a datapack registry with the given entries
     */
    private <T> DataProvider registryRL(RegistryKey<Registry<T>> registry, Map<Identifier, T> entries) {
        return JsonCodecProvider.forDatapackRegistry(this.generator, this.existingFileHelper, TConstruct.MOD_ID, this.registryOps, registry, entries);
    }

    /**
     * Creates a datapack registry with the given entries
     */
    private <T> DataProvider registryName(RegistryKey<Registry<T>> registry, Map<String, T> entries) {
        return this.registryRL(registry, entries.entrySet().stream().collect(Collectors.toMap(entry -> TConstruct.getResource(entry.getKey()), Entry::getValue)));
    }

    /**
     * Creates a datapack registry with the given entries
     */
    private <T> DataProvider registryKey(RegistryKey<Registry<T>> registry, Map<RegistryKey<T>, T> entries) {
        return this.registryRL(registry, entries.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getValue(), Entry::getValue)));
    }


    /* Object creation helpers */

    /**
     * Saves a structure set
     */
    private StructureSet structureSet(StructurePlacement placement, WeightedEntry... structures) {
        return new StructureSet(List.of(structures), placement);
    }

    /**
     * Creates an entry for a registry object
     */
    private WeightedEntry entry(RegistryKey<Structure> structure, int weight) {
        return new WeightedEntry(this.reference(structure), weight);
    }

    /**
     * Creates a spawn override for a single mob
     */
    private static Map<SpawnGroup, StructureSpawns> monsterOverride(EntityType<?> entity, int min, int max) {
        return Map.of(SpawnGroup.MONSTER, new StructureSpawns(BoundingBox.STRUCTURE, Pool.of(new SpawnSettings.SpawnEntry(entity, 1, min, max))));
    }
}
