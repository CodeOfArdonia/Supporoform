package slimeknights.tconstruct.plugin.jei.partbuilder;

import lombok.NoArgsConstructor;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
public class PatternIngredientRenderer implements IIngredientRenderer<Pattern> {
    public static final PatternIngredientRenderer INSTANCE = new PatternIngredientRenderer();

    @Override
    public void render(MatrixStack matrices, @Nullable Pattern pattern) {
        if (pattern != null) {
            Sprite sprite = MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).getSprite(pattern.getTexture());
            RenderUtils.setup(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            Screen.blit(matrices, 0, 0, 100, 16, 16, sprite);
        }
    }

    @Override
    public List<Text> getTooltip(Pattern pattern, TooltipContext flag) {
        if (flag.isAdvanced()) {
            return Arrays.asList(pattern.getDisplayName(), Text.literal(pattern.toString()).formatted(Formatting.DARK_GRAY));
        } else {
            return Collections.singletonList(pattern.getDisplayName());
        }
    }
}
