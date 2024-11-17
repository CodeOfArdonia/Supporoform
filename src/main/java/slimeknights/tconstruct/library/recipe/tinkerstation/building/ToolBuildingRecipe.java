package slimeknights.tconstruct.library.recipe.tinkerstation.building;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.util.LogicHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.tables.TinkerTables;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * This recipe is used for crafting a set of parts into a tool
 */

@RequiredArgsConstructor
public class ToolBuildingRecipe implements ITinkerStationRecipe {
    public static final RecordLoadable<ToolBuildingRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(),
            LoadableRecipeSerializer.RECIPE_GROUP,
            TinkerLoadables.MODIFIABLE_ITEM.requiredField("result", r -> r.output),
            IntLoadable.FROM_ONE.defaultField("result_count", 1, true, r -> r.outputCount),
            Loadables.RESOURCE_LOCATION.nullableField("slot_layout", r -> r.layoutSlot),
            IngredientLoadable.DISALLOW_EMPTY.list(0).defaultField("extra_requirements", List.of(), r -> r.ingredients),
            ToolBuildingRecipe::new);

    @Getter
    protected final Identifier id;
    @Getter
    protected final String group;
    @Getter
    protected final IModifiable output;
    protected final int outputCount;
    @Nullable
    protected final Identifier layoutSlot;
    protected final List<Ingredient> ingredients;
    protected List<LayoutSlot> layoutSlots;
    protected List<List<ItemStack>> allToolParts;
    public static final int X_OFFSET = -6;
    public static final int Y_OFFSET = -15;
    public static final int SLOT_SIZE = 18;

    /**
     * @deprecated Use {@link #ToolBuildingRecipe(ResourceLocation, String, IModifiable, int, ResourceLocation, List)}
     */
    @Deprecated
    public ToolBuildingRecipe(Identifier id, String group, IModifiable output, int outputCount, List<Ingredient> ingredients) {
        this(id, group, output, outputCount, null, ingredients);
    }

    /**
     * Gets the ID of the station slot layout for displaying this recipe.
     * Typically matches the output definition ID, but some tool recipes share a single layout.
     */
    public Identifier getLayoutSlotId() {
        return Objects.requireNonNullElse(this.layoutSlot, this.getOutput().getToolDefinition().getId());
    }

    /**
     * Gets the layout slots so we know where go position item slots for guis
     */
    public List<LayoutSlot> getLayoutSlots() {
        if (this.layoutSlots == null) {
            this.layoutSlots = StationSlotLayoutLoader.getInstance().get(this.getLayoutSlotId()).getInputSlots();
            if (this.layoutSlots.isEmpty()) {
                // fallback to tinker station or anvil
                this.layoutSlots = StationSlotLayoutLoader.getInstance().get(TConstruct.getResource(this.requiresAnvil() ? "tinkers_anvil" : "tinker_station")).getInputSlots();
            }
            int missingSlots = this.getAllToolParts().size() + this.getExtraRequirements().size() - this.layoutSlots.size();
            // check layout slots if its too small
            if (missingSlots > 0) {
                TConstruct.LOG.error(String.format("Tool part count is greater than layout slot count for %s!", this.getId()));
                this.layoutSlots = new ArrayList<>(this.layoutSlots);
                for (int additionalSlot = 0; additionalSlot < missingSlots; additionalSlot++) {
                    this.layoutSlots.add(new LayoutSlot(null, null, additionalSlot * SLOT_SIZE - X_OFFSET, -Y_OFFSET, null));
                }
            }
        }
        return this.layoutSlots;
    }

    /**
     * Gets the tool parts for this tool
     */
    public List<IToolPart> getToolParts() {
        return ToolPartsHook.parts(this.getOutput().getToolDefinition());
    }

    /**
     * Gets all tool parts as and all its variants for JEI input lookups.
     */
    public List<List<ItemStack>> getAllToolParts() {
        if (this.allToolParts == null) {
            this.allToolParts = this.getToolParts().stream()
                    .map(part -> MaterialRegistry.getInstance().getVisibleMaterials().stream()
                            .filter(part::canUseMaterial)
                            .map(mat -> part.withMaterial(mat.getIdentifier()))
                            .toList())
                    .toList();
        }
        return this.allToolParts;
    }

    /**
     * Gets the additional recipe requirements beyond the tool parts
     */
    public List<Ingredient> getExtraRequirements() {
        return this.ingredients;
    }

    /**
     * Helper to determine if an anvil is required
     */
    public boolean requiresAnvil() {
        return this.getToolParts().size() + this.getExtraRequirements().size() >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerTables.toolBuildingRecipeSerializer.get();
    }

    @Override
    public boolean matches(ITinkerStationContainer inv, World worldIn) {
        if (!inv.getTinkerableStack().isEmpty()) {
            return false;
        }
        List<IToolPart> parts = this.getToolParts();
        int requiredInputs = parts.size() + this.ingredients.size();
        int maxInputs = inv.getInputCount();
        // disallow if we have no inputs, or if we have too few slots
        if (requiredInputs == 0 || requiredInputs > maxInputs) {
            return false;
        }
        // each part must match the given slot
        int i;
        int partSize = parts.size();
        for (i = 0; i < partSize; i++) {
            if (parts.get(i).asItem() != inv.getInput(i).getItem()) {
                return false;
            }
        }
        // remaining slots must match extra requirements
        for (; i < maxInputs; i++) {
            Ingredient ingredient = LogicHelper.getOrDefault(this.ingredients, i - partSize, Ingredient.EMPTY);
            if (!ingredient.test(inv.getInput(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(ITinkerStationContainer inv) {
        // first n slots contain parts
        List<MaterialVariant> materials = IntStream.range(0, ToolPartsHook.parts(this.output.getToolDefinition()).size())
                .mapToObj(i -> MaterialVariant.of(IMaterialItem.getMaterialFromStack(inv.getInput(i))))
                .toList();
        return ToolStack.createTool(this.output.asItem(), this.output.getToolDefinition(), new MaterialNBT(materials)).createStack(this.outputCount);
    }

    /**
     * @deprecated Use {@link #assemble(ITinkerStationContainer)}
     */
    @Deprecated
    @Override
    public ItemStack getOutput(DynamicRegistryManager manager) {
        return new ItemStack(this.output);
    }
}
