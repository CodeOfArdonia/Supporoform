// Credit to Immersive Engineering and blusunrize for this class
// See: https://github.com/BluSunrize/ImmersiveEngineering/blob/1.18/src/main/java/blusunrize/immersiveengineering/common/util/fakeworld/TemplateWorld.java
package slimeknights.mantle.client.book.structure.level;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import net.minecraft.world.tick.EmptyTickSchedulers;
import net.minecraft.world.tick.QueryableTickScheduler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * World implementation for the book structures
 */
public class TemplateLevel extends World {

    private final Map<String, MapState> maps = new HashMap<>();
    private final Scoreboard scoreboard = new Scoreboard();
    private final RecipeManager recipeManager = new RecipeManager();
    private final TemplateChunkSource chunkSource;

    public TemplateLevel(List<StructureBlockInfo> blocks, Predicate<BlockPos> shouldShow) {
        super(
                new FakeLevelData(), World.OVERWORLD, Objects.requireNonNull(MinecraftClient.getInstance().world).getRegistryManager(),
                Objects.requireNonNull(MinecraftClient.getInstance().world).getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).entryOf(DimensionTypes.OVERWORLD),
                () -> DummyProfiler.INSTANCE, true, false, 0, 0
        );

        this.chunkSource = new TemplateChunkSource(blocks, this, shouldShow);
    }

    @Override
    public void updateListeners(@Nonnull BlockPos pos, @Nonnull BlockState oldState, @Nonnull BlockState newState, int flags) {
    }

    @Override
    public void playSound(@Nullable PlayerEntity pPlayer, double pX, double pY, double pZ, RegistryEntry<SoundEvent> pSound, SoundCategory pSource, float pVolume, float pPitch, long pSeed) {
    }

    @Override
    public void playSoundFromEntity(@Nullable PlayerEntity pPlayer, Entity pEntity, RegistryEntry<SoundEvent> pSound, SoundCategory pCategory, float pVolume, float pPitch, long pSeed) {
    }

    @Override
    public String asString() {
        return this.chunkSource.getDebugString();
    }

    @Nullable
    @Override
    public Entity getEntityById(int id) {
        return null;
    }

    @Nullable
    @Override
    public MapState getMapState(@Nonnull String mapName) {
        return this.maps.get(mapName);
    }

    @Override
    public void putMapState(String mapId, MapState mapDataIn) {
        this.maps.put(mapId, mapDataIn);
    }

    @Override
    public int getNextMapId() {
        return this.maps.size();
    }

    @Override
    public void setBlockBreakingInfo(int breakerId, @Nonnull BlockPos pos, int progress) {
    }

    @Nonnull
    @Override
    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    @Nonnull
    @Override
    public RecipeManager getRecipeManager() {
        return this.recipeManager;
    }

    @Override
    protected EntityLookup<Entity> getEntityLookup() {
        return FakeEntityGetter.INSTANCE;
    }

    @Nonnull
    @Override
    public QueryableTickScheduler<Block> getBlockTickScheduler() {
        return EmptyTickSchedulers.getClientTickScheduler();
    }

    @Nonnull
    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
        return EmptyTickSchedulers.getClientTickScheduler();
    }

    @Nonnull
    @Override
    public ChunkManager getChunkManager() {
        return this.chunkSource;
    }

    @Override
    public void syncWorldEvent(@Nullable PlayerEntity player, int type, @Nonnull BlockPos pos, int data) {
    }

    @Override
    public void emitGameEvent(GameEvent pEvent, Vec3d pPosition, Emitter pContext) {
    }

    @Override
    public FeatureSet getEnabledFeatures() {
        return FeatureSet.empty();
    }

    @Override
    public float getBrightness(@Nonnull Direction p_230487_1_, boolean p_230487_2_) {
        return 1;
    }

    @Nonnull
    @Override
    public List<? extends PlayerEntity> getPlayers() {
        return ImmutableList.of();
    }

    @Nonnull
    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int x, int y, int z) {
        return this.getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.PLAINS);
    }
}
