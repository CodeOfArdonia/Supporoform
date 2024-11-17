package slimeknights.mantle.registration.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.RegistryHelper;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Registry object wrapper to implement {@link ItemConvertible}
 *
 * @param <I> Item class
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@AllArgsConstructor
public class ItemObject<I extends ItemConvertible> implements Supplier<I>, ItemConvertible, IdAwareObject {
    /**
     * Supplier to the registry entry
     */
    private final RegistryEntry<? extends I> entry;
    /**
     * Registry name for this entry, allows fetching the name before the entry resolves if registry object is used
     */
    @Getter
    private final Identifier id;

    /**
     * Creates a new item object from a supplier instance. Registry name will be fetched from the supplier entry, so the entry must be present during construction
     *
     * @param entry Existing registry entry, typically a vanilla block or a registered block
     */
    public ItemObject(DefaultedRegistry<I> registry, I entry) {
        this.entry = RegistryHelper.getHolder(registry, entry);
        this.id = registry.getId(entry);
    }

    /**
     * Creates a new item object using the given registry object. This variant can resolve its name before the registry object entry resolves
     *
     * @param object Object base
     */
    public ItemObject(RegistryEntry<? extends I> object) {
        this.entry = object;
        this.id = object.getKey().get().getValue();
    }

    /**
     * Creates a new item object using another item object. Intended to be used in a subclass to avoid an extra wrapper
     *
     * @param object Object base
     */
    protected ItemObject(ItemObject<? extends I> object) {
        this.entry = object.entry;
        this.id = object.id;
    }

    /**
     * Gets the entry, throwing an exception if not present
     *
     * @return Entry
     * @throws NullPointerException if not present
     */
    @Override
    public I get() {
        return Objects.requireNonNull(this.entry.value(), () -> "Item Object not present " + this.id);
    }

    /**
     * Gets the entry, or null if its not present
     *
     * @return entry, or null if missing
     */
    @Nullable
    public I getOrNull() {
        try {
            return this.entry.value();
        } catch (NullPointerException e) {
            // thrown by RegistryObject if missing value
            return null;
        }
    }

    @Override
    public Item asItem() {
        return this.get().asItem();
    }

    @Override
    public Identifier getId() {
        return this.id;
    }
}
