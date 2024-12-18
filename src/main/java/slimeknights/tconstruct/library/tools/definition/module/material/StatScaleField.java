package slimeknights.tconstruct.library.tools.definition.module.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.data.loadable.field.RecordField;
import slimeknights.mantle.util.typed.TypedMap;

/**
 * Specialized field used for parsing stat weights for {@link MaterialStatsModule} and {@link PartStatsModule}
 */
record StatScaleField(String nestKey, String listKey) implements RecordField<float[], MaterialStatsModule> {
    @Override
    public float[] get(JsonObject json, TypedMap context) {
        JsonArray list = JsonHelper.getArray(json, this.listKey);
        float[] scales = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            JsonElement element = list.get(i);
            if (element.isJsonObject()) {
                scales[i] = JsonHelper.getFloat(element.getAsJsonObject(), "scale");
            } else {
                scales[i] = 1;
            }
        }
        return scales;
    }

    @Override
    public void serialize(MaterialStatsModule parent, JsonObject json) {
        // expect the list to be serialized before us
        JsonArray list = JsonHelper.getArray(json, this.listKey);
        int size = Math.min(list.size(), parent.scales.length);
        for (int i = 0; i < size; i++) {
            float scale = parent.scales[i];
            if (scale != 1) {
                JsonElement element = list.get(i);
                JsonObject object;
                if (element.isJsonObject()) {
                    object = element.getAsJsonObject();
                } else {
                    object = new JsonObject();
                    object.add(this.nestKey, element);
                    list.set(i, object);
                }
                object.addProperty("scale", scale);
            }
        }
    }

    @Override
    public float[] decode(PacketByteBuf buffer, TypedMap context) {
        int size = buffer.readVarInt();
        float[] scales = new float[size];
        for (int i = 0; i < size; i++) {
            scales[i] = buffer.readFloat();
        }
        return scales;
    }

    @Override
    public void encode(PacketByteBuf buffer, MaterialStatsModule parent) {
        buffer.writeVarInt(parent.scales.length);
        for (float scale : parent.scales) {
            buffer.writeFloat(scale);
        }
    }
}
