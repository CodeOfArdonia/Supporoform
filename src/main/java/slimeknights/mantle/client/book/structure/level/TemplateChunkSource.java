// Credit to Immersive Engineering and blusunrize for this class
// See: https://github.com/BluSunrize/ImmersiveEngineering/blob/1.18/src/main/java/blusunrize/immersiveengineering/common/util/fakeworld/TemplateChunkProvider.java
package slimeknights.mantle.client.book.structure.level;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TemplateChunkSource extends ChunkManager {

    private final Map<ChunkPos, Chunk> chunks;
    private final World level;
    private final LightingProvider lightManager;

    public TemplateChunkSource(List<StructureBlockInfo> blocks, World level, Predicate<BlockPos> shouldShow) {
        this.level = level;
        this.lightManager = new LightingProvider(this, true, true);
        Map<ChunkPos, List<StructureBlockInfo>> byChunk = new HashMap<>();

        for (StructureBlockInfo info : blocks) {
            byChunk.computeIfAbsent(new ChunkPos(info.pos()), $ -> new ArrayList<>()).add(info);
        }

        this.chunks = byChunk.entrySet().stream()
                .map(e -> Pair.of(e.getKey(), new TemplateChunk(level, e.getKey(), e.getValue(), shouldShow)))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @Nullable
    @Override
    public Chunk getChunk(int chunkX, int chunkZ, @Nonnull ChunkStatus requiredStatus, boolean load) {
        return this.chunks.computeIfAbsent(new ChunkPos(chunkX, chunkZ), p -> new EmptyChunk(this.level, p, this.level.getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.PLAINS)));
    }

    @Override
    public void tick(BooleanSupplier hasMoreTime, boolean tickChunks) {
    }

    @Override
    public String getDebugString() {
        return "?";
    }

    @Override
    public int getLoadedChunkCount() {
        return this.chunks.size();
    }

    @Nonnull
    @Override
    public LightingProvider getLightingProvider() {
        return this.lightManager;
    }

    @Override
    public BlockView getWorld() {
        return this.level;
    }
}
