package slimeknights.tconstruct.library.client.model.block;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import slimeknights.mantle.client.model.inventory.ModelItem;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * This model contains a list of items to display in the TESR, plus a single scalable fluid that can either be statically rendered or rendered in the TESR
 */
public class MelterModel extends TankModel {
    /**
     * Shared loader instance
     */
    public static final IGeometryLoader<TankModel> LOADER = MelterModel::deserialize;

    private final List<ModelItem> items;

    @SuppressWarnings("WeakerAccess")
    protected MelterModel(SimpleBlockModel model, @Nullable SimpleBlockModel gui, IncrementalFluidCuboid fluid, List<ModelItem> items) {
        super(model, gui, fluid, false);
        this.items = items;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext owner, ModelLoader bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location) {
        BakedModel baked = this.model.bake(owner, bakery, spriteGetter, transform, overrides, location);
        // bake the GUI model if present
        BakedModel bakedGui = baked;
        if (this.gui != null) {
            bakedGui = this.gui.bake(owner, bakery, spriteGetter, transform, overrides, location);
        }
        return new Baked(owner, transform, baked, bakedGui, this);
    }

    /**
     * Baked variant to allow access to items
     */
    public static final class Baked extends TankModel.Baked<MelterModel> {
        private Baked(IGeometryBakingContext owner, ModelBakeSettings transforms, BakedModel baked, BakedModel gui, MelterModel original) {
            super(owner, transforms, baked, gui, original);
        }

        /**
         * Gets a list of items used in inventory display
         *
         * @return Item list
         */
        public List<ModelItem> getItems() {
            return this.original.items;
        }
    }

    /**
     * Loader for this model
     */
    public static TankModel deserialize(JsonObject json, JsonDeserializationContext context) {
        SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);
        SimpleBlockModel gui = null;
        if (json.has("gui")) {
            gui = SimpleBlockModel.deserialize(JsonHelper.getObject(json, "gui"), context);
        }
        IncrementalFluidCuboid fluid = IncrementalFluidCuboid.fromJson(JsonHelper.getObject(json, "fluid"));
        List<ModelItem> items = ModelItem.listFromJson(json, "items");
        return new MelterModel(model, gui, fluid, items);
    }
}
