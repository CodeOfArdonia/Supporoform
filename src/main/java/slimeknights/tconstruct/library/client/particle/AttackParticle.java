package slimeknights.tconstruct.library.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/**
 * Simple particle used on attack
 */
public class AttackParticle extends SpriteBillboardParticle {

    private final SpriteProvider spriteList;

    public AttackParticle(ClientWorld world, double x, double y, double z, double pQuadSizeMultiplier, SpriteProvider spriteList) {
        super(world, x, y, z, 0, 0, 0);
        this.spriteList = spriteList;
        float f = this.random.nextFloat() * 0.6F + 0.4F;
        this.red = f;
        this.green = f;
        this.blue = f;
        this.maxAge = 4;
        this.scale = 1.0F - (float) pQuadSizeMultiplier * 0.5F;
        this.setSpriteForAge(spriteList);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_LIT;
    }

    @Override
    public int getBrightness(float partialTicks) {
        return 0xF000F0;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            this.setSpriteForAge(this.spriteList);
        }
    }

    /**
     * Render factory instance
     */
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteSet;

        public Factory(SpriteProvider spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(DefaultParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new AttackParticle(worldIn, x, y, z, xSpeed, this.spriteSet);
        }
    }
}
