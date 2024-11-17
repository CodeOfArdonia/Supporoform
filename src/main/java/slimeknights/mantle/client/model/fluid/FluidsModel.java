package slimeknights.mantle.client.model.fluid;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.model.BakedModelWrapper;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import java.util.List;
import java.util.function.Function;

/**
 * This model contains a list of fluid cuboids for the sake of rendering multiple fluid regions in world. It is used by the faucet at this time
 */
@AllArgsConstructor
public class FluidsModel implements IUnbakedGeometry<FluidsModel> {
    public static final IGeometryLoader<FluidsModel> LOADER = FluidsModel::deserialize;

    private final SimpleBlockModel model;
    private final List<FluidCuboid> fluids;

    @Override
    public void resolveParents(Function<Identifier, UnbakedModel> modelGetter, JsonUnbakedModel context) {
        this.model.resolveParents(modelGetter, context);
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker baker, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location, boolean b) {
        BakedModel baked = this.model.bake(owner, baker, spriteGetter, transform, overrides, location);
        return new Baked(baked, this.fluids);
    }

    /**
     * Baked model, mostly a data wrapper around a normal model
     */
    @SuppressWarnings("WeakerAccess")
    public static class Baked extends BakedModelWrapper<BakedModel> {
        @Getter
        private final List<FluidCuboid> fluids;

        public Baked(BakedModel originalModel, List<FluidCuboid> fluids) {
            super(originalModel);
            this.fluids = fluids;
        }
    }

    /**
     * Deserializes the model from JSON
     */
    public static FluidsModel deserialize(JsonObject json, JsonDeserializationContext context) {
        ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);
        List<FluidCuboid> fluid = FluidCuboid.listFromJson(json, "fluids");
        return new FluidsModel(model, fluid);
    }
}
