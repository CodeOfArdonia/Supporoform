package slimeknights.tconstruct.library.modifiers.util;

import com.google.gson.JsonObject;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.modifiers.Modifier;

import org.jetbrains.annotations.Nullable;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Shared boilerplate for a module with a nullable key that can alternatively be the modifier ID
 */
public interface ModuleWithKey {
    /**
     * Field for building loadables
     */
    LoadableField<Identifier, ModuleWithKey> FIELD = Loadables.RESOURCE_LOCATION.nullableField("key", ModuleWithKey::key);

    /**
     * Gets the key for the module
     */
    default Identifier getKey(Modifier modifier) {
        Identifier key = this.key();
        if (key != null) {
            return key;
        }
        return modifier.getId();
    }

    /**
     * Gets the key field from the record
     */
    @Nullable
    Identifier key();

    /**
     * Parses the key from JSON
     *
     * @param json Json object
     * @return Key, or null if not present
     */
    @Nullable
    static Identifier parseKey(JsonObject json) {
        if (json.has("key")) {
            return JsonHelper.getResourceLocation(json, "key");
        }
        return null;
    }

    /**
     * Reads the key from the network
     */
    @Nullable
    static Identifier fromNetwork(PacketByteBuf buffer) {
        if (buffer.readBoolean()) {
            return buffer.readIdentifier();
        }
        return null;
    }

    /**
     * Writes the key to the network
     */
    static void toNetwork(@Nullable Identifier key, PacketByteBuf buffer) {
        if (key != null) {
            buffer.writeBoolean(true);
            buffer.writeIdentifier(key);
        } else {
            buffer.writeBoolean(false);
        }
    }
}
