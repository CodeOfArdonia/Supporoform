package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Extension of the vanilla ingredient to make stack size checks
 */
@RequiredArgsConstructor(staticName = "of")
public class SizedIngredient implements Predicate<ItemStack> {
    /**
     * Empty sized ingredient wrapper. Matches only the empty stack of size 0
     */
    public static final SizedIngredient EMPTY = of(Ingredient.EMPTY, 0);

    public static final RecordLoadable<SizedIngredient> LOADABLE = RecordLoadable.create(
            IngredientLoadable.DISALLOW_EMPTY.tryDirectField("ingredient", SizedIngredient::getIngredient, "amount_needed"),
            IntLoadable.FROM_ONE.defaultField("amount_needed", 1, SizedIngredient::getAmountNeeded),
            SizedIngredient::new);

    /**
     * Ingredient to use in recipe match
     */
    @Getter
    private final Ingredient ingredient;
    /**
     * Amount of this ingredient needed
     */
    @Getter
    private final int amountNeeded;

    /**
     * Last list of matching stacks from the ingredient
     */
    private WeakReference<ItemStack[]> lastIngredientMatch;
    /**
     * Cached matching stacks from last time it was requested
     */
    private List<ItemStack> matchingStacks;

    /**
     * Gets a new sized ingredient with a size of 1
     *
     * @param ingredient Ingredient
     * @return Sized ingredient matching any size
     */
    public static SizedIngredient of(Ingredient ingredient) {
        return of(ingredient, 1);
    }

    /**
     * Gets a new sized ingredient with a size of 1
     *
     * @param amountNeeded Number that must match of this ingredient
     * @param items        List of items
     * @return Sized ingredient matching any size
     */
    public static SizedIngredient fromItems(int amountNeeded, ItemConvertible... items) {
        return of(Ingredient.ofItems(items), amountNeeded);
    }

    /**
     * Gets a new sized ingredient with a size of 1
     *
     * @param items List of items
     * @return Sized ingredient matching any size
     */
    public static SizedIngredient fromItems(ItemConvertible... items) {
        return fromItems(1, items);
    }

    /**
     * Gets a new sized ingredient with a size of 1
     *
     * @param tag          Tag to match
     * @param amountNeeded Number that must match of this ingredient
     * @return Sized ingredient matching any size
     */
    public static SizedIngredient fromTag(TagKey<Item> tag, int amountNeeded) {
        return of(Ingredient.fromTag(tag), amountNeeded);
    }

    /**
     * Gets a new sized ingredient with a size of 1
     *
     * @param tag Tag to match
     * @return Sized ingredient matching any size
     */
    public static SizedIngredient fromTag(TagKey<Item> tag) {
        return fromTag(tag, 1);
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack.getCount() >= this.amountNeeded && this.ingredient.test(stack);
    }

    /**
     * Checks if the ingredient has no matching stacks
     *
     * @return True if the ingredient has no matching stacks
     */
    public boolean isEmpty() {
        return this.ingredient.isEmpty();
    }

    /**
     * Gets a list of matching stacks for display in JEI
     *
     * @return List of matching stacks
     */
    public List<ItemStack> getMatchingStacks() {
        ItemStack[] ingredientMatch = this.ingredient.getMatchingStacks();
        // if we never cached, or the array instance changed since we last cached, recache
        if (this.matchingStacks == null || this.lastIngredientMatch.get() != ingredientMatch) {
            this.matchingStacks = Arrays.stream(ingredientMatch).map(stack -> {
                if (stack.getCount() != this.amountNeeded) {
                    stack = stack.copy();
                    stack.setCount(this.amountNeeded);
                }
                return stack;
            }).collect(Collectors.toList());
            this.lastIngredientMatch = new WeakReference<>(ingredientMatch);
        }
        return this.matchingStacks;
    }

    /**
     * Writes this ingredient to the packet buffer
     *
     * @param buffer Buffer instance
     */
    public void write(PacketByteBuf buffer) {
        LOADABLE.encode(buffer, this);
    }

    /**
     * Writes this sized ingredient to a JSON object
     *
     * @return JsonObject of sized ingredient
     */
    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        LOADABLE.serialize(this, json);
        return json;
    }

    /**
     * Reads a sized ingredient from the packet buffer
     *
     * @param buffer Buffer instance
     * @return Sized ingredient
     */
    public static SizedIngredient read(PacketByteBuf buffer) {
        return LOADABLE.decode(buffer);
    }

    /**
     * Reads a sized ingredient from JSON
     *
     * @param json JSON instance
     * @return Sized ingredient
     */
    public static SizedIngredient deserialize(JsonObject json) {
        return LOADABLE.deserialize(json);
    }
}
