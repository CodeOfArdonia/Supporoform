package slimeknights.tconstruct.library.recipe.partbuilder;

import slimeknights.tconstruct.library.utils.IdParser;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * This is a copy of resource location with a couple extra helpers
 */
public class Pattern extends Identifier {
    public static final IdParser<Pattern> PARSER = new IdParser<>(Pattern::new, "Pattern");

    public Pattern(String resourceName) {
        super(resourceName);
    }

    public Pattern(String namespaceIn, String pathIn) {
        super(namespaceIn, pathIn);
    }

    public Pattern(Identifier resourceLocation) {
        super(resourceLocation.getNamespace(), resourceLocation.getPath());
    }

    /**
     * Gets the translation key for this pattern
     *
     * @return Translation key
     */
    public String getTranslationKey() {
        return Util.makeTranslationKey("pattern", this);
    }

    /**
     * Gets the display name for this pattern
     *
     * @return Display name
     */
    public Text getDisplayName() {
        return Text.translatable(getTranslationKey());
    }

    /**
     * Gets the texture for this pattern for rendering
     *
     * @return Pattern texture
     */
    public Identifier getTexture() {
        return new Identifier(getNamespace(), "gui/tinker_pattern/" + getPath());
    }

    /**
     * Tries to create a pattern from the given string, for NBT parsing
     *
     * @param string String
     * @return Tool stat ID, or null of invalid
     */
    @Nullable
    public static Pattern tryParse(String string) {
        return PARSER.tryParse(string);
    }
}
