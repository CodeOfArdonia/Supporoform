package slimeknights.tconstruct.common.registration;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.registry.Registries;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.mantle.registration.deferred.DeferredRegisterWrapper;

import java.util.function.Supplier;

/**
 * Register for argument types that automatically handles registering with {@link ArgumentTypes#registerByClass(Class, ArgumentTypeInfo)}
 */
@SuppressWarnings("UnusedReturnValue")
public class ArgumentTypeDeferredRegister extends DeferredRegisterWrapper<ArgumentSerializer<?, ?>> {
    public ArgumentTypeDeferredRegister(String modID) {
        super(Registries.COMMAND_ARGUMENT_TYPE, modID);
    }

    /**
     * Registers an argument type
     *
     * @param name          Name of the argument
     * @param argumentClass Class of the argument
     * @param supplier      Supplier to the argument info
     * @param <A>           Argument type
     * @param <T>           Argument info template type
     * @param <I>           Argument info type
     * @return Registry object
     */
    public <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>, I extends ArgumentSerializer<A, T>> RegistryObject<I> register(String name, Class<A> argumentClass, Supplier<I> supplier) {
        return register.register(name, () -> {
            I info = supplier.get();
            ArgumentTypes.registerByClass(argumentClass, info);
            return info;
        });
    }

    /**
     * Registers a context free singleton argument
     *
     * @param name          Name of the argument
     * @param argumentClass Class of the argument
     * @param supplier      Supplier to the argument default
     * @param <A>           Argument type
     * @return Registry object
     */
    public <A extends ArgumentType<?>> RegistryObject<ConstantArgumentSerializer<A>> registerSingleton(String name, Class<A> argumentClass, Supplier<A> supplier) {
        return register(name, argumentClass, () -> ConstantArgumentSerializer.of(supplier));
    }
}
