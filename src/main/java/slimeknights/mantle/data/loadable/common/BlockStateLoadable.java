package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map.Entry;
import java.util.Optional;

/**
 * Loadable reading block state properties from JSON
 */
public enum BlockStateLoadable implements RecordLoadable<BlockState> {
    /**
     * Serializes all state properties
     */
    ALL {
        @Override
        protected <T extends Comparable<T>> void serializeProperty(BlockState serialize, Property<T> property, BlockState defaultState, JsonObject json) {
            json.addProperty(property.getName(), property.name(serialize.get(property)));
        }
    },
    /**
     * Serializes any properties different from the default state
     */
    DIFFERENCE {
        @Override
        protected <T extends Comparable<T>> void serializeProperty(BlockState serialize, Property<T> property, BlockState defaultState, JsonObject json) {
            T value = serialize.get(property);
            if (!value.equals(defaultState.get(property))) {
                json.addProperty(property.getName(), property.name(value));
            }
        }
    };

    @Override
    public BlockState convert(JsonElement element, String key) {
        // primitive means parse the block with default properties
        if (element.isJsonPrimitive()) {
            return Loadables.BLOCK.convert(element, key).getDefaultState();
        }
        return RecordLoadable.super.convert(element, key);
    }

    /**
     * Sets the property
     *
     * @param state    State before changes
     * @param property Property to set
     * @param name     Value name
     * @param <T>      Type of property
     * @return State with the property
     * @throws JsonSyntaxException if the property has no element with the given name
     */
    private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String name) {
        Optional<T> value = property.parse(name);
        if (value.isPresent()) {
            return state.with(property, value.get());
        }
        throw new JsonSyntaxException("Property " + property + " does not contain value " + name);
    }

    @Override
    public BlockState deserialize(JsonObject json, TypedMap context) {
        Block block = Loadables.BLOCK.getIfPresent(json, "block");
        BlockState state = block.getDefaultState();
        if (json.has("properties")) {
            StateManager<Block, BlockState> definition = block.getStateManager();
            for (Entry<String, JsonElement> entry : JsonHelper.getObject(json, "properties").entrySet()) {
                String key = entry.getKey();
                Property<?> property = definition.getProperty(key);
                if (property == null) {
                    throw new JsonSyntaxException("Property " + key + " does not exist in block " + block);
                }
                state = setValue(state, property, JsonHelper.asString(entry.getValue(), key));
            }
        }
        return state;
    }

    @Override
    public JsonElement serialize(BlockState state) {
        Block block = state.getBlock();
        if (this == DIFFERENCE && state == block.getDefaultState()) {
            return Loadables.BLOCK.serialize(block);
        }
        return RecordLoadable.super.serialize(state);
    }

    /**
     * Serializes the property if it differs in the default state
     */
    protected abstract <T extends Comparable<T>> void serializeProperty(BlockState serialize, Property<T> property, BlockState defaultState, JsonObject json);

    @Override
    public void serialize(BlockState state, JsonObject json) {
        Block block = state.getBlock();
        json.add("block", Loadables.BLOCK.serialize(block));
        BlockState defaultState = block.getDefaultState();
        JsonObject properties = new JsonObject();
        for (Property<?> property : block.getStateManager().getProperties()) {
            this.serializeProperty(state, property, defaultState, properties);
        }
        if (properties.size() > 0) {
            json.add("properties", properties);
        }
    }

    @Override
    public BlockState decode(PacketByteBuf buffer, TypedMap context) {
        return Block.getStateFromRawId(buffer.readVarInt());
    }

    @Override
    public void encode(PacketByteBuf buffer, BlockState object) {
        buffer.writeVarInt(Block.getRawIdFromState(object));
    }
}
