package slimeknights.mantle.client.model.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Cross between {@link BakedModelWrapper} and {@link net.minecraftforge.client.model.IDynamicBakedModel}.
 * Used to create a baked model wrapper that has a dynamic {@link #getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)} (BlockState, Direction, Random, IModelData)} without worrying about overriding the deprecated variant.
 *
 * @param <T> Baked model parent
 */
@SuppressWarnings("WeakerAccess")
public abstract class DynamicBakedWrapper<T extends BakedModel> extends BakedModelWrapper<T> {

    protected DynamicBakedWrapper(T originalModel) {
        super(originalModel);
    }

    /**
     * @deprecated use {@link #getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)}
     */
    @Override
    @Deprecated
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
        return this.getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    @NotNull
    public abstract List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, ModelData extraData, @Nullable RenderLayer renderType);
}
