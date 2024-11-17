package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.netty.handler.codec.DecoderException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import slimeknights.mantle.recipe.IMultiRecipe;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helpers used in creation of recipes
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeHelper {

    /* Recipe manager utils */

    /**
     * Gets a recipe of a specific class type by name from the manager
     *
     * @param manager Recipe manager
     * @param name    Recipe name
     * @param clazz   Output class
     * @param <C>     Return type
     * @return Optional of the recipe, or empty if the recipe is missing
     */
    public static <C extends Recipe<?>> Optional<C> getRecipe(RecipeManager manager, Identifier name, Class<C> clazz) {
        return manager.get(name).filter(clazz::isInstance).map(clazz::cast);
    }

    /**
     * Gets a list of all recipes from the manager, safely casting to the specified type. Multi Recipes are kept as a single recipe instance
     *
     * @param manager Recipe manager
     * @param type    Recipe type
     * @param clazz   Preferred recipe class type
     * @param <I>     Inventory interface type
     * @param <T>     Recipe class
     * @param <C>     Return type
     * @return List of recipes from the manager
     */
    public static <I extends Inventory, T extends Recipe<I>, C extends T> List<C> getRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz) {
        return manager.getAllOfType(type).values().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of recipes for display in a UI list, such as UI buttons. Will be sorted to keep the order the same on both sides, and filtered based on the given predicate and class
     *
     * @param manager Recipe manager
     * @param type    Recipe type
     * @param clazz   Preferred recipe class type
     * @param filter  Filter for which recipes to add to the list
     * @param <I>     Inventory interface type
     * @param <T>     Recipe class
     * @param <C>     Return type
     * @return Recipe list
     */
    public static <I extends Inventory, T extends Recipe<I>, C extends T> List<C> getUIRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz, Predicate<? super C> filter) {
        return manager.getAllOfType(type).values().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .filter(filter)
                .sorted(Comparator.comparing(Recipe::getId))
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of all recipes from the manager, expanding multi recipes. Intended for use in recipe display such as JEI
     *
     * @param recipes Stream of recipes
     * @param clazz   Preferred recipe class type
     * @param <C>     Return type
     * @return List of flattened recipes from the manager
     */
    public static <C> List<C> getJEIRecipes(Stream<? extends Recipe<?>> recipes, Class<C> clazz) {
        return recipes
                .sorted((r1, r2) -> {
                    // if one is multi, and the other not, the multi recipe is larger
                    boolean m1 = r1 instanceof IMultiRecipe<?>;
                    boolean m2 = r2 instanceof IMultiRecipe<?>;
                    if (m1 && !m2) return 1;
                    if (!m1 && m2) return -1;
                    // fall back to recipe ID
                    return r1.getId().compareTo(r2.getId());
                })
                .flatMap((recipe) -> {
                    // if its a multi recipe, extract child recipes and stream those
                    if (recipe instanceof IMultiRecipe<?>) {
                        return ((IMultiRecipe<?>) recipe).getRecipes().stream();
                    }
                    return Stream.of(recipe);
                })
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of all recipes from the manager, expanding multi recipes. Intended for use in recipe display such as JEI
     *
     * @param manager Recipe manager
     * @param type    Recipe type
     * @param clazz   Preferred recipe class type
     * @param <C>     Return type
     * @return List of flattened recipes from the manager
     */
    public static <I extends Inventory, T extends Recipe<I>, C> List<C> getJEIRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz) {
        return getJEIRecipes(manager.getAllOfType(type).values().stream(), clazz);
    }


    /* JSON */

    /**
     * Serializes the fluid stack into JSON
     *
     * @param stack Stack to serialize
     * @return JSON data
     */
    public static JsonObject serializeFluidStack(FluidStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("fluid", Registries.FLUID.getId(stack.getFluid()).toString());
        json.addProperty("amount", stack.getAmount());
        return json;
    }

    /**
     * Deserializes the fluid stack from JSON
     *
     * @param json JSON data
     * @return Fluid stack instance
     * @throws JsonSyntaxException if syntax is invalid
     */
    public static FluidStack deserializeFluidStack(JsonObject json) {
        String fluidName = JsonHelper.getString(json, "fluid");
        Fluid fluid = Registries.FLUID.get(new Identifier(fluidName));
        if (fluid == Fluids.EMPTY) {
            throw new JsonSyntaxException("Unknown fluid " + fluidName);
        }
        int amount = JsonHelper.getInt(json, "amount");
        return new FluidStack(fluid, amount);
    }

    /**
     * Gets an item from JSON and validates it class type
     *
     * @param name  String containing an item name
     * @param key   Key to use for errors
     * @param clazz Output class
     * @param <C>   Class type
     * @return Item read from JSON with the given class type
     * @throws JsonSyntaxException If the key is missing, or the value is not the right class
     */
    public static <C> C deserializeItem(String name, String key, Class<C> clazz) {
        Item item = Registries.ITEM.get(new Identifier(name));
        if (item == Items.AIR) {
            throw new JsonSyntaxException("Invalid " + key + ": Unknown item " + name + "'");
        }
        if (!clazz.isInstance(item)) {
            throw new JsonSyntaxException("Invalid " + key + ": must be " + clazz.getSimpleName());
        }
        return clazz.cast(item);
    }


    /* Packet buffer utils */

    /**
     * Reads an item from the packet buffer
     *
     * @param buffer Buffer instance
     * @return Item read from the buffer
     */
    public static Item readItem(PacketByteBuf buffer) {
        return Item.byRawId(buffer.readVarInt());
    }

    /**
     * Reads an item from the packet buffer and validates its class type
     *
     * @param buffer Buffer instance
     * @param clazz  Output class
     * @param <T>    Class type
     * @return Item read from the buffer with the given class type
     * @throws DecoderException If the value is not the right class
     */
    public static <T> T readItem(PacketByteBuf buffer, Class<T> clazz) {
        Item item = readItem(buffer);
        if (!clazz.isInstance(item)) {
            throw new DecoderException("Invalid item '" + Registries.ITEM.getId(item) + "', must be " + clazz.getSimpleName());
        }
        return clazz.cast(item);
    }

    /**
     * Writes an item to the packet buffer
     *
     * @param buffer Buffer instance
     * @param item   Item to write
     */
    public static void writeItem(PacketByteBuf buffer, ItemConvertible item) {
        buffer.writeVarInt(Item.getRawId(item.asItem()));
    }
}
