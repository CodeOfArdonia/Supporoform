// Credit to Immersive Engineering and blusunrize for this class
// See: https://github.com/BluSunrize/ImmersiveEngineering/blob/1.18/src/main/java/blusunrize/immersiveengineering/common/util/fakeworld/TemplateChunk.java
package slimeknights.mantle.client.book.structure.level;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.EmptyChunk;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TemplateChunk extends EmptyChunk {

    private final Map<BlockPos, StructureBlockInfo> blocksInChunk;
    private final Map<BlockPos, BlockEntity> tiles;
    private final Predicate<BlockPos> shouldShow;

    public TemplateChunk(World level, ChunkPos chunkPos, List<StructureBlockInfo> blocksInChunk, Predicate<BlockPos> shouldShow) {
        super(level, chunkPos, level.getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.PLAINS));
        this.shouldShow = shouldShow;
        this.blocksInChunk = new HashMap<>();
        this.tiles = new HashMap<>();

        for (StructureBlockInfo info : blocksInChunk) {
            this.blocksInChunk.put(info.pos(), info);

            if (info.nbt() != null) {
                BlockEntity tile = BlockEntity.createFromNbt(info.pos(), info.state(), info.nbt());

                if (tile != null) {
                    tile.setWorld(level);
                    this.tiles.put(info.pos(), tile);
                }
            }
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (this.shouldShow.test(pos)) {
            StructureBlockInfo result = this.blocksInChunk.get(pos);

            if (result != null)
                return result.state();
        }

        return Blocks.VOID_AIR.getDefaultState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos, CreationType creationMode) {
        if (!this.shouldShow.test(pos))
            return null;

        return this.tiles.get(pos);
    }
}
