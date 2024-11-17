package slimeknights.mantle.registration.adapter;

import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.IForgeRegistry;

@SuppressWarnings("unused")
public class ContainerTypeRegistryAdapter extends RegistryAdapter<ScreenHandlerType<?>> {
    /**
     * @inheritDoc
     */
    public ContainerTypeRegistryAdapter(Registry<ScreenHandlerType<?>> registry, String modId) {
        super(registry, modId);
    }

    /**
     * @inheritDoc
     */
    public ContainerTypeRegistryAdapter(Registry<ScreenHandlerType<?>> registry) {
        super(registry);
    }

    /**
     * Registers a container type
     *
     * @param name    Container name
     * @param factory Container factory
     * @param <C>     Container type
     * @return Registry object containing the container type
     */
    public <C extends ScreenHandler> ScreenHandlerType<C> registerType(IContainerFactory<C> factory, String name) {
        return register(IForgeMenuType.create(factory), name);
    }
}
