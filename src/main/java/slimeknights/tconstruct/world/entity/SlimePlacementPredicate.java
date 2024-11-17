package slimeknights.tconstruct.world.entity;

import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction.SpawnPredicate;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;

/**
 * Placement predicate using a slime type
 */
@RequiredArgsConstructor
public class SlimePlacementPredicate<T extends SlimeEntity> implements SpawnPredicate<T> {
    private final TagKey<Block> tag;

    @Override
    public boolean test(EntityType<T> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (reason == SpawnReason.SPAWNER) {
            return true;
        }
        return world.getBlockState(pos.down()).isIn(this.tag);
    }
}
