package slimeknights.tconstruct.library.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.recipe.helper.ItemOutput;

import java.util.Random;

/**
 * Class that represents an item that has a random element to the result
 */
public abstract class RandomItem {
    /**
     * Produces a random result with a range from the min count to the result stack size
     *
     * @param result   Result, stack size determines max output
     * @param minCount Minimum count for randomness
     * @return Random item
     */
    public static RandomItem range(ItemOutput result, int minCount) {
        return new Range(result, minCount);
    }

    /**
     * Produces a random result with a range from the 0 to the result stack size
     *
     * @param result Result, stack size determines max output
     * @return Random item
     */
    public static RandomItem range(ItemOutput result) {
        return range(result, 0);
    }

    /**
     * Produces a random result with a percent chance
     *
     * @param result Result
     * @param chance Percent chance of a result
     * @return Random item
     */
    public static RandomItem chance(ItemOutput result, float chance) {
        return new Chance(result, chance);
    }

    /**
     * Produces a constant result
     *
     * @param result Result
     * @return Random item
     */
    public static RandomItem constant(ItemOutput result) {
        return chance(result, 1.0f);
    }

    /**
     * Gets a copy of the item for the given random object
     *
     * @param random Random instance for randomization
     * @return Item stack, or empty if the random failed
     */
    public abstract ItemStack get(Random random);

    /**
     * Serializes this object to JSON
     */
    public abstract JsonElement serialize();

    /**
     * Writes this object to the packet buffer
     */
    public abstract void write(PacketByteBuf buffer);

    /**
     * Reads a random item from JSON
     *
     * @param element JSON element
     * @return Random item
     */
    public static RandomItem fromJson(JsonElement element, String name) {
        // we serialize max to count for ranges as it looks cleaner, put it back
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("max")) {
                object.addProperty("count", JsonHelper.getInt(object, "max"));
                object.remove("max");
            }
        }

        ItemOutput result = ItemOutput.Loadable.REQUIRED_STACK.convert(element, name);
        // primitive is just an item name, so constant
        if (element.isJsonPrimitive()) {
            return constant(result);
        }
        // can't handle null or array
        if (!element.isJsonObject()) {
            throw new JsonSyntaxException("Invalid RandomItem at '" + name + "', must be a string or an object");
        }
        JsonObject object = element.getAsJsonObject();
        // min and max count
        if (object.has("min")) {
            return range(result, JsonHelper.getInt(object, "min"));
        }
        // percent chance
        if (object.has("chance")) {
            return chance(result, JsonHelper.getFloat(object, "chance"));
        }
        // constant tag most likely
        return constant(result);
    }

    /**
     * Reads the object from the packet buffer
     */
    public static RandomItem read(PacketByteBuf buffer) {
        ItemOutput result = ItemOutput.read(buffer);
        RandomType type = buffer.readEnumConstant(RandomType.class);
        switch (type) {
            case RANGE -> {
                int min = buffer.readVarInt();
                return range(result, min);
            }
            case CHANCE -> {
                float chance = buffer.readFloat();
                return chance(result, chance);
            }
        }
        throw new DecoderException("Invalid random type " + type + " for RandomItem");
    }

    /**
     * Item that outputs with a uniform range
     */
    @RequiredArgsConstructor
    private static class Range extends RandomItem {
        /**
         * Item result, max count will be up to the the result stack size
         */
        private final ItemOutput result;
        /**
         * Minimum count of the item
         */
        private final int minCount;

        @Override
        public ItemStack get(Random random) {
            ItemStack result = this.result.get();
            // safety in case min count is too high
            int newCount = result.getCount();
            if (result.getCount() > minCount) {
                newCount = minCount + random.nextInt(result.getCount() - minCount);
                if (newCount <= 0) {
                    return ItemStack.EMPTY;
                }
            }
            return ItemHandlerHelper.copyStackWithSize(result, newCount);
        }

        @Override
        public JsonElement serialize() {
            JsonElement resultElement = this.result.serialize(true);
            JsonObject object;
            // if a primitive, that means its just an item name, so build an object around it
            if (resultElement.isJsonPrimitive()) {
                object = new JsonObject();
                object.add("item", resultElement);
            } else {
                object = resultElement.getAsJsonObject();
            }
            object.addProperty("min", minCount);
            object.addProperty("max", JsonHelper.getInt(object, "count", 1));
            object.remove("count");
            return object;
        }

        @Override
        public void write(PacketByteBuf buffer) {
            result.write(buffer);
            buffer.writeEnumConstant(RandomType.RANGE);
            buffer.writeVarInt(minCount);
        }
    }

    /**
     * Item that outputs with a percent chance
     */
    @RequiredArgsConstructor
    private static class Chance extends RandomItem {
        /**
         * Item result
         */
        private final ItemOutput result;
        /**
         * Percent chance of the result, from 0 to 1
         */
        private final float chance;

        @Override
        public ItemStack get(Random random) {
            if (chance >= 1.0f || random.nextFloat() < chance) {
                return result.get().copy();
            }
            return ItemStack.EMPTY;
        }

        @Override
        public JsonElement serialize() {
            JsonElement resultElement = this.result.serialize(true);
            // no chance means raw item object is enough
            if (chance >= 1.0f) {
                return resultElement;
            }
            JsonObject object;
            // if a primitive, that means its just an item name, so build an object around it
            if (resultElement.isJsonPrimitive()) {
                object = new JsonObject();
                object.add("item", resultElement);
            } else {
                object = resultElement.getAsJsonObject();
            }
            object.addProperty("chance", chance);
            return object;
        }

        @Override
        public void write(PacketByteBuf buffer) {
            result.write(buffer);
            buffer.writeEnumConstant(RandomType.CHANCE);
            buffer.writeFloat(chance);
        }
    }

    /**
     * Enum of types for packet writing
     */
    private enum RandomType {RANGE, CHANCE}
}
