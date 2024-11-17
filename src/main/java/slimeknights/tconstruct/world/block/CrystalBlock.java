package slimeknights.tconstruct.world.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrystalBlock extends Block {
    private final SoundEvent chimeSound;

    public CrystalBlock(SoundEvent chimeSound, Settings props) {
        super(props);
        this.chimeSound = chimeSound;
    }

    @Override
    public void onProjectileHit(World level, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        if (!level.isClient) {
            BlockPos pos = hit.getBlockPos();
            level.playSound(null, pos, this.getSoundGroup(state, level, pos, projectile).getHitSound(), SoundCategory.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
            level.playSound(null, pos, this.chimeSound, SoundCategory.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
        }
    }
}
