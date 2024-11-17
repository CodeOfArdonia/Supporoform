package slimeknights.tconstruct.library.client.model.block;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import lombok.Getter;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.client.model.fluid.FluidCuboid;
import slimeknights.mantle.client.model.inventory.InventoryModel;
import slimeknights.mantle.client.model.inventory.ModelItem;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import java.util.List;
import java.util.function.Function;

/**
 * This model contains a single fluid region that is scaled in the TESR, and a list of two items displayed in the TESR
 */
public class CastingModel extends InventoryModel {
    /**
     * Shared loader instance
     */
    public static final IGeometryLoader<InventoryModel> LOADER = CastingModel::deserialize;

    private final FluidCuboid fluid;

    @SuppressWarnings("WeakerAccess")
    protected CastingModel(SimpleBlockModel model, List<ModelItem> items, FluidCuboid fluid) {
        super(model, items);
        this.fluid = fluid;
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location, boolean b) {
        BakedModel baked = model.bake(owner, bakery, spriteGetter, transform, overrides, location);
        return new Baked(baked, items, this.fluid);
    }

    /**
     * Baked model, mostly a data wrapper around a normal model
     */
    public static class Baked extends InventoryModel.Baked {
        @Getter
        private final FluidCuboid fluid;

        private Baked(BakedModel originalModel, List<ModelItem> items, FluidCuboid fluid) {
            super(originalModel, items);
            this.fluid = fluid;
        }
    }

    /**
     * Deserializes this model from JSON
     */
    public static InventoryModel deserialize(JsonObject json, JsonDeserializationContext context) {
        SimpleBlockModel model = ColoredBlockModel.deserialize(json, context);
        List<ModelItem> items = ModelItem.listFromJson(json, "items");
        FluidCuboid fluid = FluidCuboid.fromJson(JsonHelper.getObject(json, "fluid"));
        return new CastingModel(model, items, fluid);
    }
}
