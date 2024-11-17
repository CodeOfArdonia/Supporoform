package slimeknights.tconstruct.tools.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.List;

/**
 * Dynamic item holding a modifier
 */
public class ModifierCrystalItem extends Item {
    private static final Text TOOLTIP_MISSING = TConstruct.makeTranslation("item", "modifier_crystal.missing").formatted(Formatting.GRAY);
    private static final Text TOOLTIP_APPLY = TConstruct.makeTranslation("item", "modifier_crystal.tooltip").formatted(Formatting.GRAY);
    private static final String MODIFIER_KEY = TConstruct.makeTranslationKey("item", "modifier_crystal.modifier_id");
    private static final String TAG_MODIFIER = "modifier";

    public ModifierCrystalItem(Settings props) {
        super(props);
    }

    @Override
    public boolean hasGlint(ItemStack pStack) {
        return true;
    }

    @Override
    public Text getName(ItemStack stack) {
        ModifierId modifier = getModifier(stack);
        if (modifier != null) {
            return Text.translatable(this.getTranslationKey(stack) + ".format", Text.translatable(Util.makeTranslationKey("modifier", modifier)));
        }
        return super.getName(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext advanced) {
        ModifierId id = getModifier(stack);
        if (id != null) {
            if (ModifierManager.INSTANCE.contains(id)) {
                tooltip.addAll(ModifierManager.INSTANCE.get(id).getDescriptionList());
            }
            tooltip.add(TOOLTIP_APPLY);
            if (advanced.isAdvanced()) {
                tooltip.add((Text.translatable(MODIFIER_KEY, id.toString())).formatted(Formatting.DARK_GRAY));
            }
        } else {
            tooltip.add(TOOLTIP_MISSING);
        }
    }

    @Nullable
    @Override
    public String getCreatorModId(ItemStack stack) {
        ModifierId modifier = getModifier(stack);
        if (modifier != null) {
            return modifier.getNamespace();
        }
        return null;
    }


    /* Helpers */

    /**
     * Creates a stack with the given modifier
     */
    public static ItemStack withModifier(ModifierId modifier, int count) {
        ItemStack stack = new ItemStack(TinkerModifiers.modifierCrystal.get(), count);
        stack.getOrCreateNbt().putString(TAG_MODIFIER, modifier.toString());
        return stack;
    }

    /**
     * Creates a stack with the given modifier
     */
    public static ItemStack withModifier(ModifierId modifier) {
        return withModifier(modifier, 1);
    }

    /**
     * Gets the modifier stored on this stack
     */
    @Nullable
    public static ModifierId getModifier(ItemStack stack) {
        NbtCompound tag = stack.getNbt();
        if (tag != null) {
            return ModifierId.tryParse(tag.getString(TAG_MODIFIER));
        }
        return null;
    }

    @Override
    public void fillItemCategory(ItemGroup category, DefaultedList<ItemStack> items) {
        if (this.allowedIn(category)) {
            ModifierRecipeLookup.getRecipeModifierList().forEach(modifier -> items.add(withModifier(modifier.getId())));
        }
    }
}
