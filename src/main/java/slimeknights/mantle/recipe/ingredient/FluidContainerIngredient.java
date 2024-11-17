package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.JsonHelper;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Ingredient that matches a container of fluid
 */
@SuppressWarnings("unused")  // API
public class FluidContainerIngredient extends AbstractIngredient {
    public static final Identifier ID = Mantle.getResource("fluid_container");
    public static final Serializer SERIALIZER = new Serializer();

    /**
     * Ingredient to use for matching
     */
    private final FluidIngredient fluidIngredient;
    /**
     * Internal ingredient to display the ingredient recipe viewers
     */
    @Nullable
    private final Ingredient display;
    private ItemStack[] displayStacks;

    protected FluidContainerIngredient(FluidIngredient fluidIngredient, @Nullable Ingredient display) {
        super(Stream.of());
        this.fluidIngredient = fluidIngredient;
        this.display = display;
    }

    /**
     * Creates an instance from a fluid ingredient with a display container
     */
    public static FluidContainerIngredient fromIngredient(FluidIngredient ingredient, Ingredient display) {
        return new FluidContainerIngredient(ingredient, display);
    }

    /**
     * Creates an instance from a fluid ingredient with no display, not recommended
     */
    public static FluidContainerIngredient fromIngredient(FluidIngredient ingredient) {
        return new FluidContainerIngredient(ingredient, null);
    }

    /**
     * Creates an instance from a fluid ingredient with a display container
     */
    public static FluidContainerIngredient fromFluid(FluidObject<?> fluid, boolean forgeTag) {
        return fromIngredient(fluid.ingredient(FluidType.BUCKET_VOLUME, forgeTag), Ingredient.ofItems(fluid));
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        // first, must have a fluid capability
        return stack != null && !stack.isEmpty() && stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().flatMap(cap -> {
            // second, must contain enough fluid
            if (cap.getTanks() == 1) {
                FluidStack contained = cap.getFluidInTank(0);
                if (!contained.isEmpty() && this.fluidIngredient.getAmount(contained.getFluid()) == contained.getAmount() && this.fluidIngredient.test(contained.getFluid())) {
                    // so far so good, from this point on we are forced to make copies as we need to try draining, so copy and fetch the copy's cap
                    ItemStack copy = ItemHandlerHelper.copyStackWithSize(stack, 1);
                    return copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
                }
            }
            return Optional.empty();
        }).filter(cap -> {
            // alright, we know it has the fluid, the question is just whether draining the fluid will give us the desired result
            Fluid fluid = cap.getFluidInTank(0).getFluid();
            int amount = this.fluidIngredient.getAmount(fluid);
            FluidStack drained = cap.drain(amount, FluidAction.EXECUTE);
            // we need an exact match, and we need the resulting container item to be the same as the item stack's container item
            return drained.getFluid() == fluid && drained.getAmount() == amount && ItemStack.matches(stack.getCraftingRemainingItem(), cap.getContainer());
        }).isPresent();
    }

    @Override
    public ItemStack[] getItems() {
        if (this.displayStacks == null) {
            // no container? unfortunately hard to display this recipe so show nothing
            if (this.display == null) {
                this.displayStacks = new ItemStack[0];
            } else {
                this.displayStacks = this.display.getMatchingStacks();
            }
        }
        return this.displayStacks;
    }

    @Override
    public JsonElement toJson() {
        JsonElement element = this.fluidIngredient.serialize();
        JsonObject json;
        if (element.isJsonObject()) {
            json = element.getAsJsonObject();
        } else {
            json = new JsonObject();
            json.add("fluid", element);
        }
        json.addProperty("type", ID.toString());
        if (this.display != null) {
            json.add("display", this.display.toJson());
        }
        return json;
    }

    @Override
    protected void invalidate() {
        super.invalidate();
        this.displayStacks = null;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return SERIALIZER;
    }

    /**
     * Serializer logic
     */
    private static class Serializer implements IIngredientSerializer<FluidContainerIngredient> {
        @Override
        public FluidContainerIngredient parse(JsonObject json) {
            FluidIngredient fluidIngredient;
            // if we have fluid and its not a primitive, then its nested
            if (json.has("fluid") && !json.get("fluid").isJsonPrimitive()) {
                fluidIngredient = FluidIngredient.LOADABLE.getIfPresent(json, "fluid");
            } else {
                fluidIngredient = FluidIngredient.LOADABLE.convert(json, "fluid");
            }
            Ingredient display = null;
            if (json.has("display")) {
                display = Ingredient.fromJson(JsonHelper.getElement(json, "display"));
            }
            return new FluidContainerIngredient(fluidIngredient, display);
        }

        @Override
        public FluidContainerIngredient parse(PacketByteBuf buffer) {
            FluidIngredient fluidIngredient = FluidIngredient.read(buffer);
            Ingredient display = null;
            if (buffer.readBoolean()) {
                display = Ingredient.fromPacket(buffer);
            }
            return new FluidContainerIngredient(fluidIngredient, display);
        }

        @Override
        public void write(PacketByteBuf buffer, FluidContainerIngredient ingredient) {
            ingredient.fluidIngredient.write(buffer);
            if (ingredient.display != null) {
                buffer.writeBoolean(true);
                ingredient.display.write(buffer);
            } else {
                buffer.writeBoolean(false);
            }
        }
    }
}
