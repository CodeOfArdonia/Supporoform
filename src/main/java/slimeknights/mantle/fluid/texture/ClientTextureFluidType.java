package slimeknights.mantle.fluid.texture;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link IClientFluidTypeExtensions} using {@link FluidTexture}
 */
@SuppressWarnings("ClassCanBeRecord") // Want to allow extending to override other properties
@RequiredArgsConstructor
public class ClientTextureFluidType implements IClientFluidTypeExtensions {
    protected final FluidType type;

    @Override
    public int getTintColor() {
        return FluidTextureManager.getColor(this.type);
    }

    @Override
    public Identifier getStillTexture() {
        return FluidTextureManager.getStillTexture(this.type);
    }

    @Override
    public Identifier getFlowingTexture() {
        return FluidTextureManager.getFlowingTexture(this.type);
    }

    @Nullable
    @Override
    public Identifier getOverlayTexture() {
        return FluidTextureManager.getOverlayTexture(this.type);
    }

    @Nullable
    @Override
    public Identifier getRenderOverlayTexture(MinecraftClient mc) {
        return FluidTextureManager.getCameraTexture(this.type);
    }
}
