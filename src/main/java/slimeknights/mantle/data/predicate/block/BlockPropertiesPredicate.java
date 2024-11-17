package slimeknights.mantle.data.predicate.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.*;
import io.netty.handler.codec.DecoderException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Predicate matching a block with the given properties
 *
 * @param block      Block to match
 * @param properties Properties to match
 */
public record BlockPropertiesPredicate(Block block, List<Matcher> properties) implements BlockPredicate {
    private static final Function<String, RuntimeException> JSON_EXCEPTION = JsonSyntaxException::new;
    private static final Function<String, RuntimeException> DECODER_EXCEPTION = DecoderException::new;

    @Override
    public boolean matches(BlockState input) {
        if (input.getBlock() != this.block) {
            return false;
        }
        for (Matcher matcher : this.properties) {
            if (!matcher.matches(input)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public IGenericLoader<? extends BlockPredicate> getLoader() {
        return LOADER;
    }

    /**
     * Parses a property from the given state definition
     */
    private static Property<?> parseProperty(Block block, String name, Function<String, RuntimeException> exception) {
        Property<?> property = block.getStateManager().getProperty(name);
        if (property == null) {
            throw exception.apply("Property " + name + " does not exist in block " + Registries.BLOCK.getId(block));
        }
        return property;
    }

    /**
     * Loader instance
     */
    public static final RecordLoadable<BlockPropertiesPredicate> LOADER = new RecordLoadable<>() {
        @Override
        public BlockPropertiesPredicate deserialize(JsonObject json, TypedMap context) {
            Block block = Loadables.BLOCK.getIfPresent(json, "block");
            // TODO: this is a bit of a unique case for matcher, as its parsing from a map into a list, think about whether we can do something generic
            ImmutableList.Builder<Matcher> builder = ImmutableList.builder();
            for (Entry<String, JsonElement> entry : net.minecraft.util.JsonHelper.getObject(json, "properties").entrySet()) {
                Property<?> property = parseProperty(block, entry.getKey(), JSON_EXCEPTION);
                builder.add(Matcher.deserialize(property, entry.getValue()));
            }
            return new BlockPropertiesPredicate(block, builder.build());
        }

        @Override
        public void serialize(BlockPropertiesPredicate object, JsonObject json) {
            json.add("block", Loadables.BLOCK.serialize(object.block));
            JsonObject properties = new JsonObject();
            for (Matcher matcher : object.properties) {
                properties.add(matcher.property().getName(), matcher.serialize());
            }
            json.add("properties", properties);
        }

        @Override
        public BlockPropertiesPredicate decode(PacketByteBuf buffer, TypedMap context) {
            Block block = Loadables.BLOCK.decode(buffer);
            int size = buffer.readVarInt();
            ImmutableList.Builder<Matcher> builder = ImmutableList.builder();
            for (int i = 0; i < size; i++) {
                builder.add(Matcher.fromNetwork(block, buffer));
            }
            return new BlockPropertiesPredicate(block, builder.build());
        }

        @Override
        public void encode(PacketByteBuf buffer, BlockPropertiesPredicate object) {
            Loadables.BLOCK.encode(buffer, object.block);
            buffer.writeVarInt(object.properties.size());
            for (Matcher matcher : object.properties) {
                matcher.toNetwork(buffer);
            }
        }
    };

    /**
     * Interface of nested matcher classes
     */
    public sealed interface Matcher {
        /**
         * Returns true if the given block matches the given property
         */
        boolean matches(BlockState state);

        /**
         * Gets the property for this matcher
         */
        Property<?> property();

        /**
         * Serializes the match to a json element
         */
        JsonElement serialize();

        /**
         * Writes this to the network
         */
        void toNetwork(PacketByteBuf buffer);

        /**
         * Deserializes the value from JSON
         */
        private static <T extends Comparable<T>> T parseValue(Property<T> property, String name, Function<String, RuntimeException> exception) {
            Optional<T> value = property.parse(name);
            if (value.isPresent()) {
                return value.get();
            }
            throw exception.apply("Unknown property value " + name);
        }

        /**
         * Deserializes the property from JSON
         *
         * @param element  Json to deserialize
         * @param property Property to use
         * @param <T>      Property type
         * @return Matcher instance
         */
        static <T extends Comparable<T>> Matcher deserialize(Property<T> property, JsonElement element) {
            // if a value type, exact match. To reduce code, just use the set matcher with size 1
            if (element.isJsonPrimitive()) {
                return new SetMatcher<>(property, parseValue(property, net.minecraft.util.JsonHelper.asString(element, property.getName()), JSON_EXCEPTION));
            }
            // if an array, set match
            if (element.isJsonArray()) {
                return new SetMatcher<>(property, ImmutableSet.copyOf(JsonHelper.parseList(
                        element.getAsJsonArray(), property.getName(), (e, key) -> parseValue(property, net.minecraft.util.JsonHelper.asString(e, key), JSON_EXCEPTION)))
                );
            }
            // object means range match
            if (element.isJsonObject()) {
                JsonObject json = element.getAsJsonObject();
                T min = null;
                T max = null;
                if (json.has("min")) {
                    min = parseValue(property, net.minecraft.util.JsonHelper.getString(json, "min"), JSON_EXCEPTION);
                }
                if (json.has("max")) {
                    max = parseValue(property, net.minecraft.util.JsonHelper.getString(json, "max"), JSON_EXCEPTION);
                }
                if (min == null) {
                    if (max == null) {
                        throw new JsonSyntaxException("Either min or max must be set for a range matcher");
                    }
                } else if (min.equals(max)) {
                    // if equal, might as well use set matcher to save effort
                    return new SetMatcher<>(property, min);
                }
                return new RangeMatcher<>(property, min, max);
            }
            throw new JsonSyntaxException("Invalid matcher type " + net.minecraft.util.JsonHelper.getType(element));
        }

        /**
         * Parses a matcher from the buffer
         *
         * @param block  Block to search for the property
         * @param buffer Buffer instance before reading property name
         * @return Matcher instance
         */
        static Matcher fromNetwork(Block block, PacketByteBuf buffer) {
            Property<?> property = parseProperty(block, buffer.readString(Short.MAX_VALUE), DECODER_EXCEPTION);
            return fromNetwork(property, buffer);
        }

        /**
         * Parses a matcher from the buffer
         *
         * @param property Matcher property
         * @param buffer   Buffer instance after reading property name
         * @return Matcher instance
         */
        static <T extends Comparable<T>> Matcher fromNetwork(Property<T> property, PacketByteBuf buffer) {
            int size = buffer.readVarInt();
            // 0 means range match
            if (size == 0) {
                T min = null;
                T max = null;
                RangeType rangeType = buffer.readEnumConstant(RangeType.class);
                if (rangeType != RangeType.MAX) {
                    min = parseValue(property, buffer.readString(Short.MAX_VALUE), DECODER_EXCEPTION);
                }
                if (rangeType != RangeType.MIN) {
                    max = parseValue(property, buffer.readString(Short.MAX_VALUE), DECODER_EXCEPTION);
                }
                return new RangeMatcher<>(property, min, max);
            } else {
                ImmutableSet.Builder<T> builder = ImmutableSet.builder();
                for (int i = 0; i < size; i++) {
                    builder.add(parseValue(property, buffer.readString(Short.MAX_VALUE), DECODER_EXCEPTION));
                }
                return new SetMatcher<>(property, builder.build());
            }
        }
    }

    /**
     * Matches on a value being in a set.
     *
     * @param property Property to match
     * @param values   Set of values, must not be empty
     */
    public record SetMatcher<T extends Comparable<T>>(Property<T> property, Set<T> values) implements Matcher {
        public SetMatcher {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("Values must not be empty");
            }
        }

        public SetMatcher(Property<T> property, T value) {
            this(property, Set.of(value));
        }

        @Override
        public boolean matches(BlockState state) {
            return this.values.contains(state.get(this.property));
        }

        @Override
        public JsonElement serialize() {
            // if only a single element, simplify serialization
            if (this.values.size() == 1) {
                return new JsonPrimitive(this.property.name(this.values.iterator().next()));
            }
            // if more than 1 element, store in an array
            JsonArray array = new JsonArray();
            for (T value : this.values) {
                array.add(this.property.name(value));
            }
            return array;
        }

        @Override
        public void toNetwork(PacketByteBuf buffer) {
            buffer.writeString(this.property.getName());
            // size of 0 represents range matcher, size above 0 means set matcher
            buffer.writeVarInt(this.values.size());
            // only way we know to sync the property and values is as strings, inefficient but mojang never made properties buffer friendly
            for (T value : this.values) {
                buffer.writeString(this.property.name(value));
            }
        }
    }

    /**
     * Helper to deal with the range matcher network serializing
     */
    private enum RangeType {
        FULL, MIN, MAX;

        /**
         * Creates a ranged type from the given values, doubles as a validator for the arguments
         */
        public static RangeType fromValues(@Nullable Object min, @Nullable Object max) {
            if (max == null) {
                if (min == null) {
                    throw new IllegalArgumentException("Cannot have both min and max null");
                }
                return MIN;
            } else if (min == null) {
                return MAX;
            } else {
                return FULL;
            }
        }
    }

    /**
     * Matches on a range of values
     */
    public record RangeMatcher<T extends Comparable<T>>(Property<T> property, @Nullable T min,
                                                        @Nullable T max) implements Matcher {
        public RangeMatcher {
            RangeType.fromValues(min, max);
        }

        @Override
        public boolean matches(BlockState state) {
            T value = state.get(this.property);
            // null means ignore that part of the range
            return (this.min == null || value.compareTo(this.min) >= 0) && (this.max == null || value.compareTo(this.max) <= 0);
        }

        @Override
        public JsonElement serialize() {
            JsonObject json = new JsonObject();
            if (this.min != null) {
                json.addProperty("min", this.property.name(this.min));
            }
            if (this.max != null) {
                json.addProperty("max", this.property.name(this.max));
            }
            return json;
        }

        @Override
        public void toNetwork(PacketByteBuf buffer) {
            buffer.writeString(this.property.getName());
            // 0 means it' a range match, anything above 0 is the set size
            buffer.writeVarInt(0);
            buffer.writeEnumConstant(RangeType.fromValues(this.min, this.max));
            if (this.min != null) {
                buffer.writeString(this.property.name(this.min));
            }
            if (this.max != null) {
                buffer.writeString(this.property.name(this.max));
            }
        }
    }

    /**
     * Creates a builder instance
     */
    public static Builder block(Block block) {
        return new Builder(block);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {
        private final Block block;
        private final Map<Property<?>, Matcher> matchers = new LinkedHashMap<>();

        /**
         * Adds a matcher to the builder
         */
        private Builder matches(Matcher matcher) {
            Property<?> property = matcher.property();
            // validate the property is part of the block
            if (!this.block.getStateManager().getProperties().contains(property)) {
                throw new IllegalArgumentException("Property " + property + " does not exist in block " + this.block);
            }
            // validate we don't have the same property twice, messes with JSON serialization
            Matcher original = this.matchers.put(property, matcher);
            if (original != null) {
                throw new IllegalArgumentException("Matcher for property already exists: previous matcher " + original);
            }
            return this;
        }


        /* Set match */

        /**
         * Matches on the given set values
         */
        public <T extends Comparable<T>> Builder matches(Property<T> property, Set<T> values) {
            return this.matches(new SetMatcher<>(property, values));
        }

        /**
         * Matches on the given set values
         */
        @SafeVarargs
        public final <T extends Comparable<T>> Builder matches(Property<T> property, T... values) {
            return this.matches(property, Set.of(values));
        }


        /* Range match */

        /**
         * Matches values between min and max (inclusive)
         */
        public <T extends Comparable<T>> Builder range(Property<T> property, T min, T max) {
            if (Objects.equals(min, max)) {
                return this.matches(property, min);
            }
            return this.matches(new RangeMatcher<>(property, min, max));
        }

        /**
         * Matches values greater than or equal to min
         */
        public <T extends Comparable<T>> Builder min(Property<T> property, T min) {
            return this.matches(new RangeMatcher<>(property, min, null));
        }

        /**
         * Matches values less than or equal to max
         */
        public <T extends Comparable<T>> Builder max(Property<T> property, T max) {
            return this.matches(new RangeMatcher<>(property, null, max));
        }

        /**
         * Builds the final instance
         */
        public BlockPropertiesPredicate build() {
            if (this.matchers.isEmpty()) {
                throw new IllegalArgumentException("Must have at least one property");
            }
            return new BlockPropertiesPredicate(this.block, ImmutableList.copyOf(this.matchers.values()));
        }
    }
}
