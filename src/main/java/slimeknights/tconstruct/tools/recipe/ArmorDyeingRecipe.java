package slimeknights.tconstruct.tools.recipe;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags.Items;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recipe to dye travelers gear
 */
public class ArmorDyeingRecipe implements ITinkerStationRecipe, IMultiRecipe<IDisplayModifierRecipe> {
    public static final RecordLoadable<ArmorDyeingRecipe> LOADER = RecordLoadable.create(ContextKey.ID.requiredField(), IngredientLoadable.DISALLOW_EMPTY.requiredField("tools", r -> r.toolRequirement), ArmorDyeingRecipe::new);

    @Getter
    private final Identifier id;
    private final Ingredient toolRequirement;

    public ArmorDyeingRecipe(Identifier id, Ingredient toolRequirement) {
        this.id = id;
        this.toolRequirement = toolRequirement;
        ModifierRecipeLookup.addRecipeModifier(null, TinkerModifiers.dyed);
    }

    @Override
    public boolean matches(ITinkerStationContainer inv, World world) {
        // ensure this modifier can be applied
        if (!this.toolRequirement.test(inv.getTinkerableStack())) {
            return false;
        }
        // slots must be only dyes, and have at least 1 dye
        boolean found = false;
        for (int i = 0; i < inv.getInputCount(); i++) {
            ItemStack input = inv.getInput(i);
            if (!input.isEmpty()) {
                if (!input.isIn(Items.DYES)) {
                    return false;
                }
                found = true;
            }
        }
        return found;
    }

    @Override
    public ItemStack assemble(ITinkerStationContainer inv) {
        ToolStack tool = inv.getTinkerable().copy();

        ModDataNBT persistentData = tool.getPersistentData();
        Identifier key = TinkerModifiers.dyed.getId();
        int nr = 0, nb = 0, ng = 0;
        int brightness = 0;
        int count = 0;

        // copy existing color
        if (persistentData.contains(key, NbtElement.INT_TYPE)) {
            int color = persistentData.getInt(key);
            int r = color >> 16 & 255;
            int g = color >> 8 & 255;
            int b = color & 255;
            brightness = Math.max(r, Math.max(g, b));
            nr = r;
            nb = b;
            ng = g;
            count++;
        }

        // copy color from each dye
        for (int i = 0; i < inv.getInputCount(); i++) {
            ItemStack stack = inv.getInput(i);
            if (!stack.isEmpty()) {
                DyeColor dye = DyeColor.getColor(stack);
                if (dye != null) {
                    float[] color = dye.getColorComponents();
                    int r = (int) (color[0] * 255);
                    int g = (int) (color[1] * 255);
                    int b = (int) (color[2] * 255);
                    brightness += Math.max(r, Math.max(g, b));
                    nr += r;
                    ng += g;
                    nb += b;
                    count++;
                }
            }
        }

        // should never happen, but lets not crash
        if (count == 0) {
            return ItemStack.EMPTY;
        }

        // build the final color
        nr /= count;
        ng /= count;
        nb /= count;
        float scaledBrightness = (float) brightness / (float) count;
        brightness = Math.max(nr, Math.max(ng, nb));
        nr = (int) ((float) nr * scaledBrightness / brightness);
        ng = (int) ((float) ng * scaledBrightness / brightness);
        nb = (int) ((float) nb * scaledBrightness / brightness);
        int finalColor = (nr << 16) | (ng << 8) | nb;
        persistentData.putInt(key, finalColor);

        // add the modifier if missing
        ModifierId modifier = TinkerModifiers.dyed.getId();
        if (tool.getModifierLevel(modifier) == 0) {
            tool.addModifier(modifier, 1);
        }
        return tool.createStack(Math.min(inv.getTinkerableSize(), this.shrinkToolSlotBy()));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.armorDyeingSerializer.get();
    }


    /* JEI */

    @Nullable
    private List<IDisplayModifierRecipe> displayRecipes;

    @Override
    public List<IDisplayModifierRecipe> getRecipes() {
        if (this.displayRecipes == null) {
            List<ItemStack> toolInputs = Arrays.stream(this.toolRequirement.getMatchingStacks()).map(stack -> {
                if (stack.getItem() instanceof IModifiableDisplay) {
                    return ((IModifiableDisplay) stack.getItem()).getRenderTool();
                }
                return stack;
            }).toList();
            ModifierEntry result = new ModifierEntry(TinkerModifiers.dyed.get(), 1);
            this.displayRecipes = Arrays.stream(DyeColor.values()).map(dye -> new DisplayRecipe(result, toolInputs, dye)).collect(Collectors.toList());
        }
        return this.displayRecipes;
    }


    /* Required */

    /**
     * @deprecated use {@link #assemble(ITinkerStationContainer)}
     */
    @Deprecated
    @Override
    public ItemStack getOutput(DynamicRegistryManager manager) {
        return ItemStack.EMPTY;
    }

    /**
     * Finished recipe
     */
    @SuppressWarnings("ClassCanBeRecord")
    @RequiredArgsConstructor
    public static class Finished implements RecipeJsonProvider {
        @Getter
        private final Identifier id;
        private final Ingredient toolRequirement;

        @Override
        public void serialize(JsonObject json) {
            json.add("tools", this.toolRequirement.toJson());
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TinkerModifiers.armorDyeingSerializer.get();
        }

        @Nullable
        @Override
        public JsonObject toAdvancementJson() {
            return null;
        }

        @Nullable
        @Override
        public Identifier getAdvancementId() {
            return null;
        }
    }

    private static class DisplayRecipe implements IDisplayModifierRecipe {
        /**
         * Cache of tint colors to save calculating it twice
         */
        private static final int[] TINT_COLORS = new int[16];

        /**
         * Gets the tint color for the given dye
         */
        private static int getTintColor(DyeColor color) {
            int id = color.getId();
            // taking advantage of the fact no color is pure black
            if (TINT_COLORS[id] == 0) {
                float[] colors = color.getColorComponents();
                TINT_COLORS[id] = ((int) (colors[0] * 255) << 16) | ((int) (colors[1] * 255) << 8) | (int) (colors[2] * 255);
            }
            return TINT_COLORS[id];
        }

        private final List<ItemStack> dyes;
        @Getter
        private final ModifierEntry displayResult;
        @Getter
        private final List<ItemStack> toolWithoutModifier;
        @Getter
        private final List<ItemStack> toolWithModifier;
        @Getter
        private final Text variant;

        public DisplayRecipe(ModifierEntry result, List<ItemStack> tools, DyeColor color) {
            this.displayResult = result;
            this.toolWithoutModifier = tools;
            this.dyes = RegistryHelper.getTagValueStream(Registry.ITEM, color.getTag()).map(ItemStack::new).toList();
            this.variant = Text.translatable("color.minecraft." + color.asString());

            Identifier id = result.getModifier().getId();
            int tintColor = getTintColor(color);
            List<ModifierEntry> results = List.of(result);
            this.toolWithModifier = tools.stream().map(stack -> IDisplayModifierRecipe.withModifiers(stack, results, data -> data.putInt(id, tintColor))).toList();
        }

        @Override
        public int getInputCount() {
            return 1;
        }

        @Override
        public List<ItemStack> getDisplayItems(int slot) {
            if (slot == 0) {
                return this.dyes;
            }
            return Collections.emptyList();
        }

        @Override
        public IntRange getLevel() {
            return new IntRange(1, 1);
        }
    }
}
