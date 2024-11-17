package slimeknights.tconstruct.world.worldgen.islands;

import com.mojang.serialization.Codec;
import lombok.Getter;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Locale;

/**
 * Represents island height generation options
 */
public enum IslandPlacement implements StringIdentifiable {
    /**
     * Island that generates in the air
     */
    SKY {
        @Override
        int getHeight(ChunkPos chunkPos, ChunkGenerator generator, HeightLimitView pLevel, BlockRotation rotation, Random random, NoiseConfig randomState) {
            int xOffset;
            int yOffset;
            switch (rotation) {
                case CLOCKWISE_90 -> {
                    xOffset = -5;
                    yOffset = 5;
                }
                case CLOCKWISE_180 -> {
                    xOffset = -5;
                    yOffset = -5;
                }
                case COUNTERCLOCKWISE_90 -> {
                    xOffset = 5;
                    yOffset = -5;
                }
                default -> {
                    xOffset = 5;
                    yOffset = 5;
                }
            }

            // determine height
            int x = chunkPos.getOffsetX(7);
            int z = chunkPos.getOffsetZ(7);
            int minXMinZ = generator.getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, pLevel, randomState);
            int minXMaxZ = generator.getHeightInGround(x, z + yOffset, Heightmap.Type.WORLD_SURFACE_WG, pLevel, randomState);
            int maxXMinZ = generator.getHeightInGround(x + xOffset, z, Heightmap.Type.WORLD_SURFACE_WG, pLevel, randomState);
            int maxXMaxZ = generator.getHeightInGround(x + xOffset, z + yOffset, Heightmap.Type.WORLD_SURFACE_WG, pLevel, randomState);
            // from the smallest of the 4 positions, add 60 plus another random 50, limit to 20 blocks below world height (tallest island is 13 blocks, 7 blocks for trees)
            return Math.min(Math.min(Math.min(minXMinZ, minXMaxZ), Math.min(maxXMinZ, maxXMaxZ)) + 60 + random.nextInt(50), generator.getWorldHeight() - 20);
        }
    },
    /**
     * Island that generates on the ocean surface
     */
    SEA {
        @Override
        int getHeight(ChunkPos chunkPos, ChunkGenerator generator, HeightLimitView pLevel, BlockRotation rotation, Random random, NoiseConfig randomState) {
            return Math.max(generator.getSeaLevel() - 7, 0);
        }

        @Override
        public boolean isPositionValid(StructureWorldAccess world, BlockPos pos, ChunkGenerator generator) {
            BlockPos up = pos.up();
            if (isFluidOrEmpty(world, up)) {
                for (Direction direction : Type.HORIZONTAL) {
                    if (!isFluidOrEmpty(world, up.offset(direction))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    public static final Codec<IslandPlacement> CODEC = StringIdentifiable.createCodec(IslandPlacement::values);

    /**
     * Checks if the given position is either empty or a fluid block
     */
    private static boolean isFluidOrEmpty(StructureWorldAccess world, BlockPos pos) {
        return world.isAir(pos) || world.getBlockState(pos).getBlock() instanceof FluidBlock;
    }

    @Getter
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);

    /**
     * Checks if the given position is valid for this island
     */
    public boolean isPositionValid(StructureWorldAccess world, BlockPos pos, ChunkGenerator generator) {
        return true;
    }

    /**
     * Gets the height to generate this island
     */
    abstract int getHeight(ChunkPos chunkPos, ChunkGenerator generator, HeightLimitView pLevel, BlockRotation rotation, Random random, NoiseConfig randomState);
}
