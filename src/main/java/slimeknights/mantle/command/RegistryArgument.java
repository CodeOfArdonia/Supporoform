package slimeknights.mantle.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Argument type that supports any vanilla registry. Due to the lack of context, not a true argument type but rather helpers
 */
public class RegistryArgument {
    /* Name is invalid */
    private static final DynamicCommandExceptionType NOT_FOUND = new DynamicCommandExceptionType(name -> Text.translatable("command.mantle.registry.not_found", name));

    /**
     * Creates an argument instance
     */
    public static ArgumentType<Identifier> registry() {
        return IdentifierArgumentType.identifier();
    }

    /**
     * Gets the result of this argument
     */
    public static Registry<?> getResult(CommandContext<? extends CommandSource> pContext, String pName) throws CommandSyntaxException {
        Identifier name = pContext.getArgument(pName, Identifier.class);
        return pContext.getSource().getRegistryManager()
                .getOptional(RegistryKey.ofRegistry(name))
                .orElseThrow(() -> NOT_FOUND.create(name));
    }
}
