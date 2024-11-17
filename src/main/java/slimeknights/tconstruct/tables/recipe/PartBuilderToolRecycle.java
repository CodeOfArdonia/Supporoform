package slimeknights.tconstruct.tables.recipe;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ingredient.SizedIngredient;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderContainer;
import slimeknights.tconstruct.library.recipe.partbuilder.IPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Recipe to break a tool into tool parts
 */
@SuppressWarnings("deprecation")  // Forge is dumb
@RequiredArgsConstructor
public class PartBuilderToolRecycle implements IPartBuilderRecipe {
    /**
     * Title for the screen
     */
    private static final Text TOOL_RECYCLING = TConstruct.makeTranslation("recipe", "tool_recycling");
    /**
     * General instructions for recycling
     */
    private static final List<Text> INSTRUCTIONS = Collections.singletonList(TConstruct.makeTranslation("recipe", "tool_recycling.info"));
    /**
     * Error for trying to recycle a tool that cannot be
     */
    private static final List<Text> NO_MODIFIERS = Collections.singletonList(TConstruct.makeTranslation("recipe", "tool_recycling.no_modifiers").formatted(Formatting.RED));
    /**
     * Default tool field
     */
    public static final SizedIngredient DEFAULT_TOOLS = SizedIngredient.fromTag(TinkerTags.Items.MULTIPART_TOOL);
    /**
     * Loader instance
     */
    public static final RecordLoadable<PartBuilderToolRecycle> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(),
            SizedIngredient.LOADABLE.defaultField("tools", DEFAULT_TOOLS, true, r -> r.toolRequirement),
            IngredientLoadable.DISALLOW_EMPTY.requiredField("pattern", r -> r.pattern),
            PartBuilderToolRecycle::new);

    /**
     * Should never be needed, but just in case better than null
     */
    private static final Pattern ERROR = new Pattern(TConstruct.MOD_ID, "missingno");
    @Getter
    private final Identifier id;
    private final SizedIngredient toolRequirement;
    private final Ingredient pattern;

    @Override
    public Pattern getPattern() {
        return ERROR;
    }

    @Override
    public Stream<Pattern> getPatterns(IPartBuilderContainer inv) {
        if (inv.getStack().getItem() instanceof IModifiable modifiable) {
            return ToolPartsHook.parts(modifiable.getToolDefinition()).stream()
                    .map(part -> Registries.ITEM.getId(part.asItem()))
                    .distinct()
                    .map(Pattern::new);
        }
        return Stream.empty();
    }

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public int getItemsUsed(IPartBuilderContainer inv) {
        return toolRequirement.getAmountNeeded();
    }

    @Override
    public boolean partialMatch(IPartBuilderContainer inv) {
        return pattern.test(inv.getPatternStack()) && toolRequirement.test(inv.getStack());
    }

    @Override
    public boolean matches(IPartBuilderContainer inv, World pLevel) {
        return partialMatch(inv) && ToolStack.from(inv.getStack()).getUpgrades().isEmpty();
    }

    @Override
    public ItemStack assemble(IPartBuilderContainer inv, Pattern pattern) {
        ToolStack tool = ToolStack.from(inv.getStack());
        // first, try to find a matching part
        IToolPart match = null;
        int matchIndex = -1;
        List<IToolPart> requirements = ToolPartsHook.parts(tool.getDefinition());
        for (int i = 0; i < requirements.size(); i++) {
            IToolPart part = requirements.get(i);
            if (pattern.equals(Registries.ITEM.getId(part.asItem()))) {
                matchIndex = i;
                match = part;
                break;
            }
        }
        // failed to find part? should never happen but safety return
        if (match == null) {
            return ItemStack.EMPTY;
        }
        return match.withMaterial(tool.getMaterial(matchIndex).getVariant());
    }

    @Override
    public ItemStack getLeftover(IPartBuilderContainer inv, Pattern pattern) {
        ToolStack tool = ToolStack.from(inv.getStack());

        // if the tool is damaged, it we only have a chance of a second tool part
        int damage = tool.getDamage();
        if (damage > 0) {
            int max = tool.getStats().getInt(ToolStats.DURABILITY);
            if (TConstruct.RANDOM.nextInt(max) < damage) {
                return ItemStack.EMPTY;
            }
        }

        // find all parts that did not match the pattern
        List<IToolPart> parts = new ArrayList<>();
        IntList indices = new IntArrayList();
        boolean found = false;
        List<IToolPart> requirements = ToolPartsHook.parts(tool.getDefinition());
        for (int i = 0; i < requirements.size(); i++) {
            IToolPart part = requirements.get(i);
            if (found || !pattern.equals(Registries.ITEM.getId(part.asItem()))) {
                parts.add(part);
                indices.add(i);
            } else {
                found = true;
            }
        }
        if (parts.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = TConstruct.RANDOM.nextInt(parts.size());
        return parts.get(index).withMaterial(tool.getMaterial(indices.getInt(index)).getVariant());
    }

    /**
     * @deprecated use {@link #assemble(IPartBuilderContainer, Pattern)}
     */
    @Deprecated
    @Override
    public ItemStack getOutput(DynamicRegistryManager manager) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerTables.partBuilderToolRecycling.get();
    }

    @Nullable
    @Override
    public Text getTitle() {
        return TOOL_RECYCLING;
    }

    @Override
    public List<Text> getText(IPartBuilderContainer inv) {
        return ModifierUtil.hasUpgrades(inv.getStack()) ? NO_MODIFIERS : INSTRUCTIONS;
    }

    @RequiredArgsConstructor
    public static class Finished implements RecipeJsonProvider {
        @Getter
        private final Identifier id;
        private final SizedIngredient tools;
        private final Ingredient pattern;

        @Override
        public void serialize(JsonObject json) {
            json.add("tools", tools.serialize());
            json.add("pattern", pattern.toJson());
        }

        @Override
        public Identifier getRecipeId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TinkerTables.partBuilderToolRecycling.get();
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
}
