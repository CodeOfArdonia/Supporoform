package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Item ingredient matching items with a block form in the given tag
 */
@RequiredArgsConstructor
public class BlockTagIngredient extends AbstractIngredient {
    private final TagKey<Block> tag;
    @Nullable
    private Set<Item> matchingItems;
    @Nullable
    private ItemStack[] items;
    @Nullable
    private IntList stackingIds;

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return stack != null && getMatchingItems().contains(stack.getItem());
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    protected void invalidate() {
        this.matchingItems = null;
        this.items = null;
        this.stackingIds = null;
    }

    /**
     * Gets the ordered matching items set
     */
    private Set<Item> getMatchingItems() {
        if (matchingItems == null || checkInvalidation()) {
            markValid();
            matchingItems = RegistryHelper.getTagValueStream(Registry.BLOCK, tag)
                    .map(Block::asItem)
                    .filter(item -> item != Items.AIR)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return matchingItems;
    }

    @Override
    public ItemStack[] getItems() {
        if (items == null || checkInvalidation()) {
            markValid();
            items = getMatchingItems().stream().map(ItemStack::new).toArray(ItemStack[]::new);
        }
        return items;
    }

    @Override
    public IntList getStackingIds() {
        if (stackingIds == null || checkInvalidation()) {
            markValid();
            Set<Item> items = getMatchingItems();
            this.stackingIds = new IntArrayList(items.size());
            for (Item item : items) {
                this.stackingIds.add(Registry.ITEM.getId(item));
            }
            this.stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
        }
        return this.stackingIds;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", Serializer.ID.toString());
        json.add("tag", Loadables.BLOCK_TAG.serialize(this.tag));
        return json;
    }

    /**
     * Serializer instance
     */
    public enum Serializer implements IIngredientSerializer<Ingredient> {
        INSTANCE;

        public static final Identifier ID = TConstruct.getResource("block_tag");

        @Override
        public Ingredient parse(JsonObject json) {
            return new BlockTagIngredient(Loadables.BLOCK_TAG.getIfPresent(json, "tag"));
        }

        @Override
        public void write(PacketByteBuf buffer, Ingredient ingredient) {
            // just write the item list, will become a vanilla ingredient client side
            buffer.writeCollection(Arrays.asList(ingredient.getMatchingStacks()), PacketByteBuf::writeItemStack);
        }

        @Override
        public Ingredient parse(PacketByteBuf buffer) {
            int size = buffer.readVarInt();
            return Ingredient.ofEntries(Stream.generate(() -> new Ingredient.StackEntry(buffer.readItemStack())).limit(size));
        }
    }
}
