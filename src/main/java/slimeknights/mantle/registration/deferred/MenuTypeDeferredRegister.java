package slimeknights.mantle.registration.deferred;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred register for menu types, automatically mapping a factory argument in {@link IForgeMenuType}
 */
@SuppressWarnings("unused")
public class MenuTypeDeferredRegister extends DeferredRegisterWrapper<ScreenHandlerType<?>> {

    public MenuTypeDeferredRegister(String modID) {
        super(RegistryKeys.SCREEN_HANDLER, modID);
    }

    /**
     * Registers a container type
     *
     * @param name    Container name
     * @param factory Container factory
     * @param <C>     Container type
     * @return Registry object containing the container type
     */
    public <C extends ScreenHandler> RegistryObject<ScreenHandlerType<C>> register(String name, IContainerFactory<C> factory) {
        return this.register.register(name, () -> IForgeMenuType.create(factory));
    }
}
