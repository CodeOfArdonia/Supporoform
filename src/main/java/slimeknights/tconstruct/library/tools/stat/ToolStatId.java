package slimeknights.tconstruct.library.tools.stat;

import slimeknights.tconstruct.library.utils.IdParser;

import org.jetbrains.annotations.Nullable;
import net.minecraft.util.Identifier;

/**
 * This is just a copy of ResourceLocation for type safety.
 */
public class ToolStatId extends Identifier {
    public static final IdParser<ToolStatId> PARSER = new IdParser<>(ToolStatId::new, "Tool Stat");

    public ToolStatId(String namespaceIn, String pathIn) {
        super(namespaceIn, pathIn);
    }

    public ToolStatId(Identifier resourceLocation) {
        super(resourceLocation.getNamespace(), resourceLocation.getPath());
    }

    public ToolStatId(String value) {
        super(value);
    }

    /**
     * Tries to create a tool stat ID from the given string, for NBT parsing
     *
     * @param string String
     * @return Tool stat ID, or null of invalid
     */
    @Nullable
    public static ToolStatId tryParse(String string) {
        return PARSER.tryParse(string);
    }
}
