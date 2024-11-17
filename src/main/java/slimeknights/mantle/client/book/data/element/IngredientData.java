package slimeknights.mantle.client.book.data.element;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.mantle.client.book.repository.BookRepository;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class IngredientData implements IDataElement {
    public SizedIngredient[] ingredients = new SizedIngredient[0];
    public String action;

    private transient String error;
    private transient DefaultedList<ItemStack> items;
    private transient boolean customData;

    public DefaultedList<ItemStack> getItems() {
        return this.items;
    }

    public static IngredientData getItemStackData(ItemStack stack) {
        IngredientData data = new IngredientData();
        data.items = DefaultedList.ofSize(1, stack);
        data.customData = true;

        return data;
    }

    public static IngredientData getItemStackData(DefaultedList<ItemStack> items) {
        IngredientData data = new IngredientData();
        data.items = items;
        data.customData = true;

        return data;
    }

    @Override
    public void load(BookRepository source) {
        if (this.customData) {
            return;
        }

        ArrayList<ItemStack> stacks = new ArrayList<>();
        for (SizedIngredient ingredient : this.ingredients) {
            if (ingredient == null) {
                continue;
            }

            stacks.addAll(ingredient.getMatchingStacks());
        }

        if (this.ingredients == null || stacks.isEmpty() || !StringHelper.isEmpty(this.error)) {
            this.items = DefaultedList.ofSize(1, this.getMissingItem());
            return;
        }

        this.items = DefaultedList.copyOf(this.getMissingItem(), stacks.toArray(new ItemStack[0]));
    }

    private ItemStack getMissingItem() {
        return this.getMissingItem(this.error);
    }

    private ItemStack getMissingItem(String error) {
        ItemStack missingItem = new ItemStack(Items.BARRIER);

        NbtCompound display = missingItem.getOrCreateSubNbt("display");
        display.putString("Name", "\u00A7rError Loading Item");
        NbtList lore = new NbtList();
        if (!StringHelper.isEmpty(error)) {
            lore.add(NbtString.of("\u00A7r\u00A7eError:"));
            lore.add(NbtString.of("\u00A7r\u00A7e" + error));
        }
        display.put("Lore", lore);

        return missingItem;
    }

    public static class Deserializer implements JsonDeserializer<IngredientData> {
        @Override
        public IngredientData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            IngredientData data = new IngredientData();

            if (json.isJsonArray()) {
                JsonArray array = json.getAsJsonArray();
                data.ingredients = new SizedIngredient[array.size()];

                for (int i = 0; i < array.size(); i++) {
                    try {
                        data.ingredients[i] = this.readIngredient(array.get(i));
                    } catch (Exception e) {
                        data.ingredients[i] = SizedIngredient.of(Ingredient.ofStacks(data.getMissingItem(e.getMessage())));
                    }
                }

                return data;
            }

            try {
                data.ingredients = new SizedIngredient[]{this.readIngredient(json)};
            } catch (Exception e) {
                data.error = e.getMessage();
                return data;
            }

            if (json.isJsonObject()) {
                JsonObject object = json.getAsJsonObject();
                if (object.has("action")) {
                    JsonElement action = object.get("action");
                    if (action.isJsonPrimitive()) {
                        JsonPrimitive primitive = action.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            data.action = primitive.getAsString();
                        }
                    }
                }
            }

            return data;
        }

        private SizedIngredient readIngredient(JsonElement json) {
            if (json.isJsonPrimitive()) {
                JsonPrimitive primitive = json.getAsJsonPrimitive();

                if (primitive.isString()) {
                    Item item = ForgeRegistries.ITEMS.getValue(new Identifier(primitive.getAsString()));
                    return SizedIngredient.fromItems(item);
                }
            }

            if (!json.isJsonObject()) {
                throw new JsonParseException("Must be an array, string or JSON object");
            }

            JsonObject object = json.getAsJsonObject();
            return SizedIngredient.deserialize(object);
        }
    }
}
