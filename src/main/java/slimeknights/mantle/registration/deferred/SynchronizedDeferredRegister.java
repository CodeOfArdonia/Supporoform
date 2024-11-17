package slimeknights.mantle.registration.deferred;

import lombok.RequiredArgsConstructor;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Deferred register instance that synchronizes register calls
 */
@RequiredArgsConstructor(staticName = "create")
public class SynchronizedDeferredRegister<T> {
    private final DeferredRegister<T> internal;

    /**
     * Creates a new instance for the given resource key
     */
    public static <T> SynchronizedDeferredRegister<T> create(RegistryKey<? extends Registry<T>> key, String modid) {
        return create(DeferredRegister.create(key, modid));
    }

    /**
     * Creates a new instance for the given forge registry
     */
    public static <B> SynchronizedDeferredRegister<B> create(IForgeRegistry<B> registry, String modid) {
        return create(DeferredRegister.create(registry, modid));
    }

    /**
     * Registers the given object, synchronized over the internal register
     */
    public <I extends T> RegistryObject<I> register(final String name, final Supplier<? extends I> sup) {
        synchronized (this.internal) {
            return this.internal.register(name, sup);
        }
    }

    /**
     * Registers the internal register with the event bus
     */
    public void register(IEventBus bus) {
        this.internal.register(bus);
    }
}
