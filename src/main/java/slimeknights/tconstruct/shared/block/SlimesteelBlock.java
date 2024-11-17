package slimeknights.tconstruct.shared.block;

import com.iafenvoy.uranus.object.DamageUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SlimesteelBlock extends Block {

    public SlimesteelBlock(Settings properties) {
        super(properties);
    }

    @Override
    public void onLandedUpon(World worldIn, BlockState state, BlockPos pos, Entity entityIn, float fallDistance) {
        if (entityIn.bypassesLandingEffects()) {
            super.onLandedUpon(worldIn, state, pos, entityIn, fallDistance);
        } else {
            entityIn.handleFallDamage(fallDistance, 0.0F, DamageUtil.build(entityIn, DamageTypes.FALL));
        }
    }

    @Override
    public void onEntityLand(BlockView worldIn, Entity entity) {
        if (entity.bypassesLandingEffects()) {
            super.onEntityLand(worldIn, entity);
        } else {
            Vec3d vector3d = entity.getVelocity();
            if (vector3d.y < 0) {
                double d0 = entity instanceof LivingEntity ? 0.75 : 0.6;
                entity.setVelocity(vector3d.x, -vector3d.y * d0, vector3d.z);
            }
        }
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView worldIn, BlockPos pos, NavigationType type) {
        return false;
    }
}
