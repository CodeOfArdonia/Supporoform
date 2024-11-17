package slimeknights.tconstruct.shared.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.tconstruct.shared.particle.FluidParticleData;

/**
 * Particle type that renders a fluid still texture
 */
public class FluidParticle extends SpriteBillboardParticle {
    private final FluidStack fluid;
    private final float uCoord;
    private final float vCoord;

    protected FluidParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ, FluidStack fluid) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.fluid = fluid;
        IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(fluid.getFluid());
        this.setSprite(MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).getSprite(attributes.getStillTexture(fluid)));
        this.gravityStrength = 1.0F;
        int color = attributes.getTintColor(fluid);
        this.alpha = ((color >> 24) & 0xFF) / 255f;
        this.red = ((color >> 16) & 0xFF) / 255f;
        this.green = ((color >> 8) & 0xFF) / 255f;
        this.blue = (color & 0xFF) / 255f;
        this.scale /= 2.0F;
        this.uCoord = this.random.nextFloat() * 3.0F;
        this.vCoord = this.random.nextFloat() * 3.0F;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.TERRAIN_SHEET;
    }

    @Override
    protected float getMinU() {
        return this.sprite.getFrameU((this.uCoord + 1.0F) / 4.0F * 16.0F);
    }

    @Override
    protected float getMaxU() {
        return this.sprite.getFrameU(this.uCoord / 4.0F * 16.0F);
    }

    @Override
    protected float getMinV() {
        return this.sprite.getFrameV(this.vCoord / 4.0F * 16.0F);
    }

    @Override
    protected float getMaxV() {
        return this.sprite.getFrameV((this.vCoord + 1.0F) / 4.0F * 16.0F);
    }

    @Override
    public int getBrightness(float partialTick) {
        return FluidRenderer.withBlockLight(super.getBrightness(partialTick), this.fluid.getFluid().getFluidType().getLightLevel(this.fluid));
    }

    /**
     * Factory to create a fluid particle
     */
    public static class Factory implements ParticleFactory<FluidParticleData> {
        @Override
        public Particle createParticle(FluidParticleData data, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            FluidStack fluid = data.getFluid();
            return !fluid.isEmpty() ? new FluidParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, fluid) : null;
        }
    }
}
