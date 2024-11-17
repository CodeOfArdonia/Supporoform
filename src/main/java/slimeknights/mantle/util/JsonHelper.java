package slimeknights.mantle.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.common.BlockStateLoadable;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.mantle.network.packet.ISimplePacket;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utilities to help in parsing JSON
 */
@SuppressWarnings("unused")
public class JsonHelper {
    private JsonHelper() {
    }

    /**
     * Default GSON instance, use instead of creating a new instance unless you need additional type adapaters
     */
    public static final Gson DEFAULT_GSON = (new GsonBuilder())
            .registerTypeAdapter(Identifier.class, new Identifier.Serializer())
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * Gets an element from JSON, throwing an exception if missing
     *
     * @param json       Object parent
     * @param memberName Name to get
     * @return JsonElement found
     * @throws JsonSyntaxException if element is missing
     */
    public static JsonElement getElement(JsonObject json, String memberName) {
        if (json.has(memberName)) {
            return json.get(memberName);
        } else {
            throw new JsonSyntaxException("Missing " + memberName);
        }
    }

    /**
     * Parses a list from an JsonArray
     *
     * @param array  Json array
     * @param name   Json key of the array
     * @param mapper Mapper from the element object and name to new object
     * @param <T>    Output type
     * @return List of output objects
     */
    public static <T> List<T> parseList(JsonArray array, String name, BiFunction<JsonElement, String, T> mapper) {
        if (array.size() == 0) {
            throw new JsonSyntaxException(name + " must have at least 1 element");
        }
        // build the list
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (int i = 0; i < array.size(); i++) {
            builder.add(mapper.apply(array.get(i), name + "[" + i + "]"));
        }
        return builder.build();
    }

    /**
     * Parses a list from an JsonArray
     *
     * @param array  Json array
     * @param name   Json key of the array
     * @param mapper Mapper from the json object to new object
     * @param <T>    Output type
     * @return List of output objects
     */
    public static <T> List<T> parseList(JsonArray array, String name, Function<JsonObject, T> mapper) {
        return parseList(array, name, (element, s) -> mapper.apply(net.minecraft.util.JsonHelper.asObject(element, s)));
    }

    /**
     * Parses a list from an JsonArray
     *
     * @param parent Parent JSON object
     * @param name   Json key of the array
     * @param mapper Mapper from raw type to new object
     * @param <T>    Output type
     * @return List of output objects
     */
    public static <T> List<T> parseList(JsonObject parent, String name, BiFunction<JsonElement, String, T> mapper) {
        return parseList(net.minecraft.util.JsonHelper.getArray(parent, name), name, mapper);
    }

    /**
     * Parses a list from an JsonArray
     *
     * @param parent Parent JSON object
     * @param name   Json key of the array
     * @param mapper Mapper from json object to new object
     * @param <T>    Output type
     * @return List of output objects
     */
    public static <T> List<T> parseList(JsonObject parent, String name, Function<JsonObject, T> mapper) {
        return parseList(net.minecraft.util.JsonHelper.getArray(parent, name), name, mapper);
    }

    /**
     * Gets a resource location from JSON, throwing a nice exception if invalid
     *
     * @param json JSON object
     * @param key  Key to fetch
     * @return Resource location parsed
     */
    public static Identifier getResourceLocation(JsonObject json, String key) {
        String text = net.minecraft.util.JsonHelper.getString(json, key);
        Identifier location = Identifier.tryParse(text);
        if (location == null) {
            throw new JsonSyntaxException("Expected " + key + " to be a Resource location, was '" + text + "'");
        }
        return location;
    }

    /**
     * Gets a resource location from JSON, throwing a nice exception if invalid
     *
     * @param json     JSON object
     * @param key      Key to fetch
     * @param fallback Fallback if key is not present
     * @return Resource location parsed
     */
    public static Identifier getResourceLocation(JsonObject json, String key, Identifier fallback) {
        if (json.has(key)) {
            return getResourceLocation(json, key);
        }
        return fallback;
    }

    /**
     * Gets a resource location from JSON, throwing a nice exception if invalid
     *
     * @param json JSON object
     * @param key  Key to fetch
     * @return Resource location parsed
     */
    public static Identifier convertToResourceLocation(JsonElement json, String key) {
        String text = net.minecraft.util.JsonHelper.asString(json, key);
        Identifier location = Identifier.tryParse(text);
        if (location == null) {
            throw new JsonSyntaxException("Expected " + key + " to be a resource location, was '" + text + "'");
        }
        return location;
    }

    /**
     * Parses a registry entry from JSON
     *
     * @param registry Registry
     * @param element  Element to deserialize
     * @param key      Json key
     * @param <T>      Object type
     * @return Registry value
     * @throws JsonSyntaxException If something failed to parse
     */
    public static <T> T convertToEntry(Registry<T> registry, JsonElement element, String key) {
        Identifier name = JsonHelper.convertToResourceLocation(element, key);
        if (registry.containsId(name)) {
            T value = registry.get(name);
            if (value != null) {
                return value;
            }
        }
        throw new JsonSyntaxException("Unknown " + registry.getRegistryName() + " " + name);
    }

    /**
     * Parses a registry entry from JSON
     *
     * @param registry Registry
     * @param parent   Parent JSON object
     * @param key      Json key
     * @param <T>      Object type
     * @return Registry value
     * @throws JsonSyntaxException If something failed to parse
     */
    public static <T> T getAsEntry(Registry<T> registry, JsonObject parent, String key) {
        return convertToEntry(registry, JsonHelper.getElement(parent, key), key);
    }

    /**
     * Parses an enum from its name
     */
    private static <T extends Enum<T>> T enumByName(String name, Class<T> enumClass) {
        for (T value : enumClass.getEnumConstants()) {
            if (value.name().toLowerCase(Locale.ROOT).equals(name)) {
                return value;
            }
        }
        throw new JsonSyntaxException("Invalid " + enumClass.getSimpleName() + " " + name);
    }

    /**
     * Gets an enum value from its string name
     */
    public static <T extends Enum<T>> T convertToEnum(JsonElement element, String key, Class<T> enumClass) {
        String name = net.minecraft.util.JsonHelper.asString(element, key);
        return enumByName(name, enumClass);
    }

    /**
     * Gets an enum value from its string name
     */
    public static <T extends Enum<T>> T getAsEnum(JsonObject json, String key, Class<T> enumClass) {
        String name = net.minecraft.util.JsonHelper.getString(json, key);
        return enumByName(name, enumClass);
    }

    /**
     * Parses a color as a string
     *
     * @param color Color to parse
     * @return Parsed string
     * @deprecated use {@link ColorLoadable#parseString(String, String)}
     */
    @Deprecated(forRemoval = true)
    public static int parseColor(@Nullable String color) {
        if (color == null || color.isEmpty()) {
            return -1;
        }
        return ColorLoadable.ALPHA.parseString(color, "[unknown]");
    }


    /* Resource loaders */

    /**
     * Converts the resource into a JSON file
     *
     * @param resource Resource to read. Closed when done
     * @return JSON object, or null if failed to parse
     */
    @Nullable
    public static JsonObject getJson(Resource resource, Identifier location) {
        try (Reader reader = resource.getReader()) {
            return net.minecraft.util.JsonHelper.deserialize(reader);
        } catch (JsonParseException | IOException e) {
            Mantle.logger.error("Failed to load JSON from resource {} from pack '{}'", location, resource.getResourcePackName(), e);
            return null;
        }
    }

    /**
     * Gets a list of JSON objects for a single path in all domains and packs, for a language file like loader
     */
    public static List<JsonObject> getFileInAllDomainsAndPacks(ResourceManager manager, String path, @Nullable String preferredPath) {
        return manager
                .getAllNamespaces().stream()
                .filter(Identifier::isValid)
                .flatMap(namespace -> {
                    Identifier location = new Identifier(namespace, path);
                    return manager.getAllResources(location).stream()
                            .map(preferredPath != null ? resource -> {
                                Mantle.logger.warn("Using deprecated path {} in pack {} - use {}:{} instead", location, resource.getPack().getName(), location.getNamespace(), preferredPath);
                                return getJson(resource, location);
                            } : resource -> JsonHelper.getJson(resource, location));
                }).filter(Objects::nonNull).toList();
    }

    /**
     * Sends the packet to the given player
     */
    private static void sendPackets(NetworkWrapper network, ServerPlayerEntity player, ISimplePacket[] packets) {
        // on an integrated server, the modifier registries have a single instance on both the client and the server thread
        // this means syncing is unneeded, and has the side-effect of recreating all the modifier instances (which can lead to unexpected behavior)
        // as a result, integrated servers just mark fullyLoaded as true without syncing anything, side-effect is listeners may run twice on single player

        // on a dedicated server, the client is running a separate game instance, this is where we send packets, plus fully loaded should already be true
        // this event is not fired when connecting to a server
        if (!player.networkHandler.connection.isMemoryConnection()) {
            PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
            for (ISimplePacket packet : packets) {
                network.send(target, packet);
            }
        }
    }

    /**
     * Called when the player logs in to send packets
     */
    public static void syncPackets(OnDatapackSyncEvent event, NetworkWrapper network, ISimplePacket... packets) {
        // send to single player
        ServerPlayerEntity targetedPlayer = event.getPlayer();
        if (targetedPlayer != null) {
            sendPackets(network, targetedPlayer, packets);
        } else {
            // send to all players
            for (ServerPlayerEntity player : event.getPlayerList().getPlayers()) {
                sendPackets(network, player, packets);
            }
        }
    }

    /**
     * Localizes the given resource location to one within the folder
     *
     * @param path      Path to localize
     * @param folder    Folder to trim (without trailing /), it is not validated so make sure you call correctly
     * @param extension Extension to trim
     * @return Localized location
     */
    public static String localize(String path, String folder, String extension) {
        return path.substring(folder.length() + 1, path.length() - extension.length());
    }

    /**
     * Localizes the given resource location to one within the folder
     *
     * @param location  Location to localize
     * @param folder    Folder to trim (without trailing /), it is not validated so make sure you call correctly
     * @param extension Extension to trim
     * @return Localized location
     */
    public static Identifier localize(Identifier location, String folder, String extension) {
        return new Identifier(location.getNamespace(), localize(location.getPath(), folder, extension));
    }


    /* Block States */

    /**
     * Converts the given JSON element into a block state
     *
     * @param element Element to convert
     * @param key     Element key
     * @return Block state
     * @throws JsonSyntaxException if a property does not parse or the element is the wrong type
     */
    public static BlockState convertToBlockState(JsonElement element, String key) {
        return BlockStateLoadable.DIFFERENCE.convert(element, key);
    }

    /**
     * Converts the given JSON element into a block state
     *
     * @param parent Parent containing the block state
     * @param key    Element key
     * @return Block state
     * @throws JsonSyntaxException if a property does not parse or the element is missing or the wrong type
     */
    public static BlockState getAsBlockState(JsonObject parent, String key) {
        return BlockStateLoadable.DIFFERENCE.getIfPresent(parent, key);
    }

    /**
     * Converts the given JSON object into a block state
     *
     * @param json Json object containing "block" and "properties"
     * @return Block state
     * @throws JsonSyntaxException if any property name or property value is invalid
     */
    public static BlockState convertToBlockState(JsonObject json) {
        return BlockStateLoadable.DIFFERENCE.deserialize(json);
    }

    /**
     * Serializes the given block state to JSON, essentially writes all values that differ from the state.
     *
     * @param state State
     * @return JsonPrimitive of the block name if it matches the default state, JsonObject otherwise
     */
    public static JsonElement serializeBlockState(BlockState state) {
        return BlockStateLoadable.DIFFERENCE.serialize(state);
    }

    /**
     * Serializes the given block state to JSON, essentially writes all values that differ from the state
     *
     * @param state State
     * @return JsonObject containing properties that differ from the default state
     */
    public static JsonObject serializeBlockState(BlockState state, JsonObject json) {
        BlockStateLoadable.DIFFERENCE.serialize(state, json);
        return json;
    }
}
