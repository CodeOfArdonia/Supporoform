package slimeknights.mantle.registration.deferred;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

/**
 * Deferred register for menu types, automatically mapping a factory argument in {@link }
 */
@SuppressWarnings("unused")
public class MenuTypeDeferredRegister extends DeferredRegisterWrapper<ScreenHandlerType<?>> {

    public MenuTypeDeferredRegister(String modID) {
        super(modID);
    }

    /**
     * Registers a container type
     *
     * @param name    Container name
     * @param factory Container factory
     * @param <C>     Container type
     * @return Registry object containing the container type
     */
    // TODO
    public <C extends ScreenHandler> RegistryEntry<ScreenHandlerType<C>> register(String name, IContainerFactory<C> factory) {
        return this.register.register(name, () -> IForgeMenuType.create(factory));
    }
}
