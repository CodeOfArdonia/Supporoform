package slimeknights.tconstruct.plugin.jei.modifiers;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Special modifier ingredient renderer used for ingredients in the bookmark menu
 */
public enum ModifierBookmarkIngredientRenderer implements IIngredientRenderer<ModifierEntry> {
    INSTANCE;

    private static final String WRAPPER_KEY = "jei.tconstruct.modifier_ingredient";

    @Override
    public void render(MatrixStack matrixStack, @Nullable ModifierEntry entry) {
        if (entry != null) {
            ModifierIconManager.renderIcon(matrixStack, entry.getModifier(), 0, 0, 100, 16);
        }
    }

    @Override
    public List<Text> getTooltip(ModifierEntry entry, TooltipContext flag) {
        List<Text> list = new ArrayList<>();
        // not using the main method as that applies color
        list.add(Text.translatable(WRAPPER_KEY, Text.translatable(entry.getModifier().getTranslationKey())));
        list.addAll(entry.getModifier().getDescriptionList());
        if (flag.isAdvanced()) {
            list.add((Text.literal(entry.getId().toString())).formatted(Formatting.DARK_GRAY));
        }
        return list;
    }
}
