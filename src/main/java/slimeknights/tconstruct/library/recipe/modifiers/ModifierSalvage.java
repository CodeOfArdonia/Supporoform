package slimeknights.tconstruct.library.recipe.modifiers;

import lombok.Getter;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.ICustomOutputRecipe;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

import org.jetbrains.annotations.Nullable;

/**
 * Shared logic for main types of salvage recipes
 */
public class ModifierSalvage implements ICustomOutputRecipe<Inventory> {
    public static final RecordLoadable<ModifierSalvage> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(),
            IngredientLoadable.DISALLOW_EMPTY.requiredField("tools", r -> r.toolIngredient),
            IntLoadable.FROM_ONE.defaultField("max_tool_size", ITinkerStationRecipe.DEFAULT_TOOL_STACK_SIZE, r -> r.maxToolSize),
            ModifierId.PARSER.requiredField("modifier", r -> r.modifier),
            ModifierEntry.VALID_LEVEL.defaultField("level", r -> r.level),
            SlotCount.LOADABLE.nullableField("slots", r -> r.slots),
            ModifierSalvage::new);

    @Getter
    protected final Identifier id;
    /**
     * Ingredient determining tools matched by this
     */
    protected final Ingredient toolIngredient;
    /**
     * Max size of the tool for this modifier. If the tool size is smaller, the salvage bonus will be reduced
     */
    @Getter
    protected final int maxToolSize;
    /**
     * Modifier represented by this recipe
     */
    @Getter
    protected final ModifierId modifier;
    /**
     * Level for this to be applicable
     */
    protected final IntRange level;
    /**
     * Slots restored by this recipe, if null no slots are restored
     */
    @Nullable
    protected final SlotCount slots;

    public ModifierSalvage(Identifier id, Ingredient toolIngredient, int maxToolSize, ModifierId modifier, IntRange level, @Nullable SlotCount slots) {
        this.id = id;
        this.toolIngredient = toolIngredient;
        this.maxToolSize = maxToolSize;
        this.modifier = modifier;
        this.level = level;
        this.slots = slots;
        ModifierRecipeLookup.addSalvage(this);
    }

    /**
     * Checks if the given tool stack and level are applicable for this salvage
     *
     * @param stack         Tool item stack
     * @param tool          Tool stack instance, for potential extensions
     * @param originalLevel Level to check
     * @return True if this salvage is applicable
     */
    @SuppressWarnings("unused")
    public boolean matches(ItemStack stack, IToolStackView tool, int originalLevel) {
        return this.level.test(originalLevel) && this.toolIngredient.test(stack);
    }

    /**
     * Updates the tool data in light of removing this modifier
     *
     * @param tool Tool instance
     */
    public void updateTool(IToolStackView tool) {
        if (this.slots != null) {
            tool.getPersistentData().addSlots(this.slots.type(), this.slots.count());
        }
    }

    @Override
    public RecipeType<?> getType() {
        return TinkerRecipeTypes.DATA.get();
    }

    /**
     * @deprecated Use {@link #matches(ItemStack, IToolStackView, int)}
     */
    @Deprecated
    @Override
    public boolean matches(Inventory inv, World level) {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerModifiers.modifierSalvageSerializer.get();
    }
}
