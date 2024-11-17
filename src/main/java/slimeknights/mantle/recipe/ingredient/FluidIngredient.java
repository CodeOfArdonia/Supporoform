package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.IAmLoadable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.FluidStackLoadable;
import slimeknights.mantle.data.loadable.mapping.EitherLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.RegistryHelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple displayable ingredient type for fluids.
 */
@SuppressWarnings("unused")
public abstract class FluidIngredient implements IAmLoadable {
    /**
     * Empty fluid ingredient, matching empty stacks. This ingredient does not parse from JSON, use use defaulting methods if you wish to use it
     */
    public static final FluidMatch EMPTY = new FluidMatch(Fluids.EMPTY, 0);


    /* Loadables */

    /**
     * Creates a builder with set and tag
     */
    private static EitherLoadable.TypedBuilder<FluidIngredient> loadableBuilder() {
        return EitherLoadable.<FluidIngredient>typed().key("fluid", FLUID_MATCH).key("tag", TAG_MATCH).key("name", NAME_MATCH);
    }

    /**
     * Loadable for network writing of fluids
     */
    private static final Loadable<FluidIngredient> NETWORK = FluidStackLoadable.REQUIRED_STACK.list(0).flatXmap(fluids -> FluidIngredient.of(fluids.stream().map(FluidIngredient::of).toList()), FluidIngredient::getFluids);
    /**
     * Loadable for fluid matches
     */
    private static final RecordLoadable<FluidMatch> FLUID_MATCH = RecordLoadable.create(Loadables.FLUID.requiredField("fluid", i -> i.fluid), IntLoadable.FROM_ONE.requiredField("amount", i -> i.amount), FluidIngredient::of);
    /**
     * @deprecated Old key for fluid ingredients, remove sometime in 1.20 or 1.21
     */
    @Deprecated(forRemoval = true)
    private static final RecordLoadable<FluidMatch> NAME_MATCH = RecordLoadable.create(Loadables.FLUID.requiredField("name", i -> i.fluid), IntLoadable.FROM_ONE.requiredField("amount", i -> i.amount), (fluid, amount) -> {
        // TODO: is there a good way to get recipe context here? Cannot think of a way short of a global static context.
        Mantle.logger.warn("Using deprecated key 'name' for fluid ingredient, use 'fluid' instead. This will be removed in the future");
        return FluidIngredient.of(fluid, amount);
    });
    /**
     * Loadable for tag matches
     */
    private static final RecordLoadable<TagMatch> TAG_MATCH = RecordLoadable.create(Loadables.FLUID_TAG.requiredField("tag", i -> i.tag), IntLoadable.FROM_ONE.requiredField("amount", i -> i.amount), FluidIngredient::of);
    /**
     * Loadable for tag matches
     */
    private static final Loadable<Compound> COMPOUND = loadableBuilder().build(NETWORK).list(2).flatXmap(Compound::new, c -> c.ingredients);
    /**
     * Loadable for any fluid ingredient
     */
    public static final Loadable<FluidIngredient> LOADABLE = loadableBuilder().array(COMPOUND).build(NETWORK);


    /* Constructors */

    /**
     * Creates a new ingredient using the given fluid and amount
     *
     * @param fluid  Fluid to check
     * @param amount Minimum fluid amount
     * @return Fluid ingredient for this fluid
     */
    public static FluidMatch of(Fluid fluid, long amount) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return EMPTY;
        }
        return new FluidMatch(fluid, amount);
    }

    /**
     * Creates a new ingredient using the given fluidstack
     *
     * @param stack Fluid stack
     * @return Fluid ingredient for this fluid stack
     */
    public static FluidIngredient of(FluidStack stack) {
        return of(stack.getFluid(), stack.getAmount());
    }

    /**
     * Creates a new fluid ingredient from the given tag
     *
     * @param fluid  Fluid tag
     * @param amount Minimum fluid amount
     * @return Fluid ingredient from a tag
     */
    public static TagMatch of(TagKey<Fluid> fluid, long amount) {
        return new TagMatch(fluid, amount);
    }

    /**
     * Creates a new compound ingredient from the given list of ingredients
     *
     * @param ingredients Ingredient list
     * @return Compound ingredient
     */
    public static FluidIngredient of(FluidIngredient... ingredients) {
        return of(List.of(ingredients));
    }

    /**
     * Creates a new compound ingredient from the given list of ingredients
     *
     * @param ingredients Ingredient list
     * @return Compound ingredient
     */
    public static FluidIngredient of(List<FluidIngredient> ingredients) {
        if (ingredients.size() == 1) {
            return ingredients.get(0);
        }
        return new Compound(ingredients);
    }


    /**
     * Cached list of display fluids
     */
    private List<FluidStack> displayFluids;

    /**
     * Checks if the given fluid matches this ingredient
     *
     * @param fluid Fluid to check
     * @return True if the fluid matches
     */
    public abstract boolean test(Fluid fluid);

    /**
     * Gets the amount of the given fluid needed for the recipe
     *
     * @param fluid Fluid to check
     * @return Amount of the fluid needed
     */
    public abstract long getAmount(Fluid fluid);

    /**
     * Checks if the given fluid stack argument matches this ingredient
     *
     * @param stack Fluid stack to check
     * @return True if the fluid matches this ingredient and the amount is equal or greater than this
     */
    public boolean test(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        return stack.getAmount() >= this.getAmount(fluid) && test(stack.getFluid());
    }

    /**
     * Gets a list of fluid stacks contained in this ingredient for display
     *
     * @return List of fluid stacks for this ingredient
     */
    public List<FluidStack> getFluids() {
        if (this.displayFluids == null) {
            this.displayFluids = this.getAllFluids().stream().filter(stack -> {
                Fluid fluid = stack.getFluid();
                return fluid.isStill(fluid.getDefaultState());
            }).collect(Collectors.toList());
        }
        return this.displayFluids;
    }

    /**
     * Gets a list of fluid stacks contained in this ingredient for display, may include flowing fluids
     *
     * @return List of fluid stacks for this ingredient
     */
    protected abstract List<FluidStack> getAllFluids();

    /**
     * Serializes the Fluid Ingredient into JSON
     *
     * @return FluidIngredient JSON
     */
    public JsonElement serialize() {
        return LOADABLE.serialize(this);
    }

    /**
     * Gets the fluid ingredient from the parent and deserializes it
     */
    public static FluidIngredient deserialize(JsonObject parent, String key) {
        return LOADABLE.getIfPresent(parent, key);
    }

    /**
     * @deprecated use {@link #LOADABLE} with {@link Loadable#convert(JsonElement, String)}
     */
    @Deprecated
    public static FluidIngredient deserialize(JsonElement element, String key) {
        return LOADABLE.convert(element, key);
    }

    /**
     * Writes the ingredient into the packet buffer
     *
     * @param buffer Packet buffer instance
     */
    public void write(PacketByteBuf buffer) {
        NETWORK.encode(buffer, this);
    }

    /**
     * Reads a fluid ingredient from the packet buffer
     *
     * @param buffer Buffer instance
     * @return Fluid ingredient instance
     */
    public static FluidIngredient read(PacketByteBuf buffer) {
        return NETWORK.decode(buffer);
    }


    /**
     * Fluid ingredient that matches a single fluid
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FluidMatch extends FluidIngredient {

        private final Fluid fluid;
        private final long amount;

        @Override
        public Loadable<?> loadable() {
            return FLUID_MATCH;
        }

        @Override
        public boolean test(Fluid fluid) {
            return fluid == this.fluid;
        }

        @Override
        public long getAmount(Fluid fluid) {
            return this.amount;
        }

        @Override
        public List<FluidStack> getAllFluids() {
            return Collections.singletonList(new FluidStack(this.fluid, this.amount));
        }
    }

    /**
     * Fluid ingredient that matches a tag
     */
    @AllArgsConstructor
    private static class TagMatch extends FluidIngredient {
        private final TagKey<Fluid> tag;
        private final long amount;

        @Override
        public Loadable<?> loadable() {
            return TAG_MATCH;
        }

        @Override
        public boolean test(Fluid fluid) {
            return fluid.isIn(this.tag);
        }

        @Override
        public long getAmount(Fluid fluid) {
            return this.amount;
        }

        @Override
        public List<FluidStack> getAllFluids() {
            return RegistryHelper.getTagValueStream(Registries.FLUID, this.tag)
                    .map(fluid -> new FluidStack(fluid, this.amount))
                    .toList();
        }
    }

    /**
     * Fluid ingredient that matches a list of ingredients
     */
    @RequiredArgsConstructor
    private static class Compound extends FluidIngredient {
        private final List<FluidIngredient> ingredients;

        @Override
        public Loadable<?> loadable() {
            return COMPOUND;
        }

        @Override
        public boolean test(Fluid fluid) {
            for (FluidIngredient ingredient : this.ingredients) {
                if (ingredient.test(fluid)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean test(FluidStack stack) {
            for (FluidIngredient ingredient : this.ingredients) {
                if (ingredient.test(stack)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public long getAmount(Fluid fluid) {
            for (FluidIngredient ingredient : this.ingredients) {
                if (ingredient.test(fluid)) {
                    return ingredient.getAmount(fluid);
                }
            }
            return 0;
        }

        @Override
        public List<FluidStack> getAllFluids() {
            return this.ingredients.stream()
                    .flatMap(ingredient -> ingredient.getFluids().stream())
                    .collect(Collectors.toList());
        }
    }
}
