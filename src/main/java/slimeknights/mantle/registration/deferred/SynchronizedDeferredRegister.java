package slimeknights.mantle.registration.deferred;

import lombok.RequiredArgsConstructor;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import slimeknights.tconstruct.TConstruct;

import java.util.function.Supplier;

/**
 * Deferred register instance that synchronizes register calls
 */
@RequiredArgsConstructor(staticName = "create")
public class SynchronizedDeferredRegister<T> {

    private final Registry<T> internal;


    public SynchronizedDeferredRegister(DefaultedRegistry<T> internal) {
        this.internal = internal;
    }
    public SynchronizedDeferredRegister(Registry<T> internal) {
        this.internal = internal;
    }

    /**
     * Creates a new instance for the given resource key
     */
    public static <T> SynchronizedDeferredRegister<T> create(DefaultedRegistry<T> key, String modid) {
        return new SynchronizedDeferredRegister<T>(key);
    }

    public static <T> SynchronizedDeferredRegister<T> create(Registry<T> key, String modid) {
        return new SynchronizedDeferredRegister<T>(key);
    }

    /**
     * Registers the given object, synchronized over the internal register
     */
    public <I extends T> RegistryEntry<I> register(final String name, final Supplier<? extends I> sup) {
        synchronized (this.internal) {
            return RegistryEntry.of(Registry.register(this.internal, TConstruct.getResource(name), sup.get()));
        }
    }

    /**
     * Registers the internal register with the event bus
     */
//    public void register(IEventBus bus) {
//        this.internal.register(bus);
//    }
}
