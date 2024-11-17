package slimeknights.tconstruct.library.client.model.block;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
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
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.inventory.ModelItem;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@AllArgsConstructor
public class TableModel implements IUnbakedGeometry<TableModel> {
    /**
     * Shared loader instance
     */
    public static final IGeometryLoader<TableModel> LOADER = TableModel::deserialize;

    private final SimpleBlockModel model;
    private final Set<String> retextured;
    private final List<ModelItem> items;

    @Override
    public Collection<SpriteIdentifier> getMaterials(JsonUnbakedModel owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        return this.model.getMaterials(owner, modelGetter, missingTextureErrors);
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location, boolean b) {
        BakedModel baked = this.model.bake(owner, bakery, spriteGetter, transform, overrides, location);
        return new Baked(baked, owner, this.model, transform, RetexturedModel.getAllRetextured(owner, this.model, this.retextured), this.items);
    }

    /**
     * Baked model instance
     */
    public static class Baked extends RetexturedModel.Baked {
        @Getter
        private final List<ModelItem> items;

        protected Baked(BakedModel baked, JsonUnbakedModel owner, SimpleBlockModel model, ModelBakeSettings transform, Set<String> retextured, List<ModelItem> items) {
            super(baked, owner, model, transform, retextured);
            this.items = items;
        }
    }

    /**
     * Model deserializer
     */
    public static TableModel deserialize(JsonObject json, JsonDeserializationContext context) {
        SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);
        Set<String> retextured = RetexturedModel.getRetexturedNames(json);
        List<ModelItem> items = ModelItem.listFromJson(json, "items");
        return new TableModel(model, retextured, items);
    }
}
