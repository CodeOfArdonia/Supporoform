package slimeknights.tconstruct.world.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CongealedSlimeBlock extends Block {

    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 15, 15);

    public CongealedSlimeBlock(Settings properties) {
        super(properties);
    }

    @Deprecated
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView worldIn, BlockPos pos, NavigationType type) {
        return false;
    }

    @Nullable
    @Override
    public PathNodeType getBlockPathType(BlockState state, BlockView level, BlockPos pos, @Nullable MobEntity mob) {
        return PathNodeType.STICKY_HONEY;
    }

    @Override
    public void onEntityLand(BlockView worldIn, Entity entity) {
        if (entity.bypassesLandingEffects() || !(entity instanceof LivingEntity) && !(entity instanceof ItemEntity)) {
            super.onEntityLand(worldIn, entity);
            // this is mostly needed to prevent XP orbs from bouncing. which completely breaks the game.
            return;
        }

        Vec3d vec3d = entity.getVelocity();

        if (vec3d.y < 0) {
            double speed = entity instanceof LivingEntity ? 1.0D : 0.8D;
            entity.setVelocity(vec3d.x, -vec3d.y * speed, vec3d.z);
            entity.fallDistance = 0;
            if (entity instanceof ItemEntity) {
                entity.setOnGround(false);
            }
        } else {
            super.onEntityLand(worldIn, entity);
        }
    }

    @Override
    public void onLandedUpon(World worldIn, BlockState state, BlockPos pos, Entity entityIn, float fallDistance) {
        // no fall damage on congealed slime
        entityIn.handleFallDamage(fallDistance, 0.0F, DamageSource.FALL);
    }

    @Override
    public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
        if (!worldIn.isClient() && !entityIn.bypassesLandingEffects()) {
            Vec3d entityPosition = entityIn.getPos();
            Vec3d direction = entityPosition.subtract(pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f);
            // only bounce if within the block height, prevents bouncing on top or from two blocks vertically
            if (direction.getY() < 0.9365 && direction.getY() >= -0.0625) {
                // bounce the current speed, slightly smaller to prevent infinite bounce
                double velocity = entityPosition.subtract(entityIn.prevX, entityIn.prevY, entityIn.prevZ).length() * 0.95;
                // determine whether we bounce in the X or the Z direction, we want whichever is bigger
                Vec3d motion = entityIn.getVelocity();
                double absX = Math.abs(direction.getX());
                double absZ = Math.abs(direction.getZ());
                if (absX > absZ) {
                    // but don't bounce past the halfway point in the block, to avoid bouncing twice
                    if (absZ < 0.495) {
                        entityIn.setVelocity(new Vec3d(velocity * Math.signum(direction.getX()), motion.getY(), motion.getZ()));
                        entityIn.velocityModified = true;
                        if (velocity > 0.1) {
                            worldIn.playSound(null, pos, this.getSoundGroup(state, worldIn, pos, entityIn).getStepSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                        }
                    }
                } else {
                    if (absX < 0.495) {
                        entityIn.setVelocity(new Vec3d(motion.getX(), motion.getY(), velocity * Math.signum(direction.getZ())));
                        entityIn.velocityModified = true;
                        if (velocity > 0.1) {
                            worldIn.playSound(null, pos, this.getSoundGroup(state, worldIn, pos, entityIn).getStepSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                        }
                    }
                }
            }
        }
    }
}
