package slimeknights.mantle.fluid.transfer;

import com.google.gson.*;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.apache.commons.lang3.function.TriFunction;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * Fluid transfer info that empties a fluid from an item
 */
@RequiredArgsConstructor
public class EmptyFluidContainerTransfer implements IFluidContainerTransfer {
    public static final Identifier ID = Mantle.getResource("empty_item");

    private final Ingredient input;
    private final ItemOutput filled;
    protected final FluidStack fluid;

    @Override
    public void addRepresentativeItems(Consumer<Item> consumer) {
        for (ItemStack stack : this.input.getMatchingStacks()) {
            consumer.accept(stack.getItem());
        }
    }

    @Override
    public boolean matches(ItemStack stack, FluidStack fluid) {
        return this.input.test(stack);
    }

    /**
     * Gets the contained fluid in the given stack
     */
    protected FluidStack getFluid(ItemStack stack) {
        return this.fluid;
    }

    @Override
    public TransferResult transfer(ItemStack stack, FluidStack fluid, IFluidHandler handler) {
        FluidStack contained = this.getFluid(stack);
        int simulated = handler.fill(contained.copy(), FluidAction.SIMULATE);
        if (simulated == this.fluid.getAmount()) {
            int actual = handler.fill(contained.copy(), FluidAction.EXECUTE);
            if (actual > 0) {
                if (actual != this.fluid.getAmount()) {
                    Mantle.logger.error("Wrong amount filled from {}, expected {}, filled {}", Registries.ITEM.getId(stack.getItem()), this.fluid.getAmount(), actual);
                }
                return new TransferResult(this.filled.get().copy(), contained, false);
            }
        }
        return null;
    }

    @Override
    public JsonObject serialize(JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.add("input", this.input.toJson());
        json.add("filled", this.filled.serialize(false));
        json.add("fluid", RecipeHelper.serializeFluidStack(this.fluid));
        return json;
    }

    /**
     * Unique loader instance
     */
    public static final JsonDeserializer<EmptyFluidContainerTransfer> DESERIALIZER = new Deserializer<>(EmptyFluidContainerTransfer::new);

    /**
     * Generic deserializer
     */
    public record Deserializer<T extends EmptyFluidContainerTransfer>(
            TriFunction<Ingredient, ItemOutput, FluidStack, T> factory) implements JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            Ingredient input = Ingredient.fromJson(JsonHelper.getElement(json, "input"));
            ItemOutput filled = ItemOutput.Loadable.REQUIRED_ITEM.getIfPresent(json, "filled");
            FluidStack fluid = RecipeHelper.deserializeFluidStack(net.minecraft.util.JsonHelper.getObject(json, "fluid"));
            return this.factory.apply(input, filled, fluid);
        }
    }
}
