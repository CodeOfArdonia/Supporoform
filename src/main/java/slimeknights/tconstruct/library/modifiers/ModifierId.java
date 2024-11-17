package slimeknights.tconstruct.library.modifiers;

import slimeknights.tconstruct.library.utils.IdParser;

import org.jetbrains.annotations.Nullable;
import net.minecraft.util.Identifier;

/**
 * This is just a copy of ResourceLocation for type safety.
 */
public class ModifierId extends Identifier {
    public static final IdParser<ModifierId> PARSER = new IdParser<>(ModifierId::new, "Modifier");

    public ModifierId(String resourceName) {
        super(resourceName);
    }

    public ModifierId(String namespaceIn, String pathIn) {
        super(namespaceIn, pathIn);
    }

    public ModifierId(Identifier resourceLocation) {
        super(resourceLocation.getNamespace(), resourceLocation.getPath());
    }

    /**
     * Creates a new modifier ID from the given string
     *
     * @param string String
     * @return Material ID, or null if invalid
     */
    @Nullable
    public static ModifierId tryParse(String string) {
        return PARSER.tryParse(string);
    }
}
