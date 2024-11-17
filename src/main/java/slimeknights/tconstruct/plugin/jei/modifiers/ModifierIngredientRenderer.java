package slimeknights.tconstruct.plugin.jei.modifiers;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ModifierIngredientRenderer(int width, int height) implements IIngredientRenderer<ModifierEntry> {
    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void render(MatrixStack matrices, @Nullable ModifierEntry entry) {
        if (entry != null) {
            Text name = entry.getDisplayName();
            TextRenderer fontRenderer = getFontRenderer(MinecraftClient.getInstance(), entry);
            int x = (this.width - fontRenderer.getWidth(name)) / 2;
            fontRenderer.drawShadow(matrices, name, x, 1, -1);
        }
    }

    @Override
    public List<Text> getTooltip(ModifierEntry entry, TooltipContext flag) {
        List<Text> tooltip = entry.getModifier().getDescriptionList(entry.getLevel());
        if (flag.isAdvanced()) {
            tooltip = new ArrayList<>(tooltip);
            tooltip.add((Text.literal(entry.getId().toString())).formatted(Formatting.DARK_GRAY));
        }
        return tooltip;
    }
}
