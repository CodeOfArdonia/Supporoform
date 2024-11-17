package slimeknights.mantle.registration.deferred;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import slimeknights.tconstruct.TConstruct;

import java.util.function.Supplier;

/**
 * Deferred register for an entity, building the type from a builder instance and adding an egg
 */
@SuppressWarnings("unused")
public class EntityTypeDeferredRegister extends DeferredRegisterWrapper<EntityType<?>> {

//    private final SynchronizedDeferredRegister<Item> itemRegistry;

    public EntityTypeDeferredRegister(String modID) {
        super(modID);
//        this.itemRegistry = SynchronizedDeferredRegister.create(RegistryKeys.ITEM, modID);
    }

//    @Override
//    public void register(IEventBus bus) {
//        super.register(bus);
//        this.itemRegistry.register(bus);
//    }

    /**
     * Registers a entity type for the given entity type builder
     *
     * @param name Entity name
     * @param sup  Entity builder instance
     * @param <T>  Entity class type
     * @return Entity registry object
     */
    public <T extends Entity> RegistryEntry<EntityType<T>> register(String name, Supplier<EntityType.Builder<T>> sup) {
        return RegistryEntry.of(Registry.register(Registries.ENTITY_TYPE,TConstruct.getResource(name), sup.get().build(this.resourceName(name))));
    }

    /**
     * Registers a entity type for the given entity type builder, and registers a spawn egg for it
     *
     * @param name      Entity name
     * @param sup       Entity builder instance
     * @param primary   Primary egg color
     * @param secondary Secondary egg color
     * @param <T>       Entity class type
     * @return Entity registry object
     */
    public <T extends MobEntity> RegistryEntry<EntityType<T>> registerWithEgg(String name, Supplier<EntityType.Builder<T>> sup, int primary, int secondary) {
        RegistryEntry<EntityType<T>> object = this.register(name, sup);
//        this.itemRegistry.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(object, primary, secondary, new Item.Settings()));
        // TODO
        return object;
    }
}
