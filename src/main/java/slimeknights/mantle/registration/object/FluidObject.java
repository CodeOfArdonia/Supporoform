package slimeknights.mantle.registration.object;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import lombok.Getter;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Object containing registry entries for a fluid with no flowing form.
 *
 * @param <F> Fluid class
 * @see FlowingFluidObject
 */
@SuppressWarnings("WeakerAccess")
public class FluidObject<F extends Fluid> implements Supplier<F>, ItemConvertible, IdAwareObject {
    /**
     * Fluid name, used for tag creation
     */
    @Getter
    @NotNull
    protected final Identifier id;

    /**
     * Tag in the forge namespace, crafting equivalence
     */
    @Getter
    @NotNull
    private final TagKey<Fluid> forgeTag;
    private final Supplier<? extends FluidType> type;
    private final Supplier<? extends F> still;

    /**
     * Main constructor
     */
    public FluidObject(Identifier id, String tagName, Supplier<? extends FluidType> type, Supplier<? extends F> still) {
        this.id = id;
        this.forgeTag = TagKey.of(RegistryKeys.FLUID, new Identifier("forge", tagName));
        this.type = type;
        this.still = still;
    }

    /**
     * Gets the fluid type for this object
     */
    public FluidType getType() {
        return this.type.get();
    }

    /**
     * Gets the still form of this fluid
     *
     * @return Still form
     */
    @Override
    public F get() {
        return Objects.requireNonNull(this.still.get(), "Fluid object missing still fluid");
    }

    /**
     * Gets the bucket form of this fluid.
     *
     * @return Bucket form, or null if no bucket
     * @see #asItem()
     */
    @Nullable
    public Item getBucket() {
        Item bucket = this.still.get().getBucketItem();
        if (bucket == Items.AIR) {
            return null;
        }
        return bucket;
    }

    /**
     * Gets the bucket form of this fluid
     *
     * @return Bucket form, or air if no bucket
     * @see #getBucket()
     */
    @Override
    public Item asItem() {
        return this.still.get().getBucketItem();
    }

    /**
     * Creates an ingredient from this object
     *
     * @param amount    Ingredient amount
     * @param commonTag If true, matches the common tag, if false matches just this object
     * @return Ingredient instance
     */
    public FluidIngredient ingredient(int amount, boolean commonTag) {
        if (commonTag) {
            return FluidIngredient.of(this.get(), amount);
        }
        return FluidIngredient.of(this.getForgeTag(), amount);
    }
}
