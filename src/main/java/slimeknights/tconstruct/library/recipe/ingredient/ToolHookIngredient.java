package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ingredient that only matches tools with a specific hook
 */
public class ToolHookIngredient extends AbstractIngredient {
    private final TagKey<Item> tag;
    private final ModuleHook<?> hook;

    protected ToolHookIngredient(TagKey<Item> tag, ModuleHook<?> hook) {
        super(Stream.of(new ToolHookValue(tag, hook)));
        this.tag = tag;
        this.hook = hook;
    }

    public static ToolHookIngredient of(TagKey<Item> tag, ModuleHook<?> hook) {
        return new ToolHookIngredient(tag, hook);
    }

    public static ToolHookIngredient of(ModuleHook<?> hook) {
        return of(TinkerTags.Items.MODIFIABLE, hook);
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return stack != null && stack.isIn(this.tag) && stack.getItem() instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(this.hook);
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", Serializer.ID.toString());
        json.addProperty("tag", this.tag.id().toString());
        json.addProperty("hook", this.hook.getId().toString());
        return json;
    }

    @RequiredArgsConstructor
    public static class ToolHookValue implements Value {
        private final TagKey<Item> tag;
        private final ModuleHook<?> hook;

        @Override
        public Collection<ItemStack> getItems() {
            List<ItemStack> list = new ArrayList<>();

            // filtered version of tag values
            for (RegistryEntry<Item> holder : Registry.ITEM.getTagOrEmpty(this.tag)) {
                if (holder.value() instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(this.hook)) {
                    list.add(new ItemStack(modifiable));
                }
            }
            if (list.size() == 0) {
                list.add(new ItemStack(Blocks.BARRIER).setCustomName(Text.literal("Empty Tag: " + this.tag.id())));
            }
            return list;
        }

        @Override
        public JsonObject serialize() {
            JsonObject json = new JsonObject();
            json.addProperty("id", Serializer.ID.toString());
            json.addProperty("tag", this.tag.id().toString());
            json.addProperty("hook", this.hook.getId().toString());
            return json;
        }
    }

    /**
     * Serializer instance
     */
    public enum Serializer implements IIngredientSerializer<ToolHookIngredient> {
        INSTANCE;

        public static final Identifier ID = TConstruct.getResource("tool_hook");

        @Override
        public ToolHookIngredient parse(JsonObject json) {
            return new ToolHookIngredient(
                    Loadables.ITEM_TAG.getOrDefault(json, "tag", TinkerTags.Items.MODIFIABLE),
                    ToolHooks.LOADER.getIfPresent(json, "hook")
            );
        }

        @Override
        public ToolHookIngredient parse(PacketByteBuf buffer) {
            return new ToolHookIngredient(
                    Loadables.ITEM_TAG.decode(buffer),
                    ToolHooks.LOADER.decode(buffer)
            );
        }

        @Override
        public void write(PacketByteBuf buffer, ToolHookIngredient ingredient) {
            Loadables.ITEM_TAG.encode(buffer, ingredient.tag);
            ToolHooks.LOADER.encode(buffer, ingredient.hook);
        }
    }
}
