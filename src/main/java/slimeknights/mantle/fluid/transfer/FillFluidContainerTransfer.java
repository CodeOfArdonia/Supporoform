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
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;
import slimeknights.mantle.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * Fluid transfer info that fills a fluid into an item
 */
@RequiredArgsConstructor
public class FillFluidContainerTransfer implements IFluidContainerTransfer {
    public static final Identifier ID = Mantle.getResource("fill_item");

    private final Ingredient input;
    private final ItemOutput filled;
    private final FluidIngredient fluid;

    @Override
    public void addRepresentativeItems(Consumer<Item> consumer) {
        for (ItemStack stack : this.input.getMatchingStacks()) {
            consumer.accept(stack.getItem());
        }
    }

    @Override
    public boolean matches(ItemStack stack, FluidStack fluid) {
        return this.input.test(stack) && this.fluid.test(fluid);
    }

    /**
     * Gets the output filled with the given fluid
     */
    protected ItemStack getFilled(FluidStack drained) {
        return this.filled.get().copy();
    }

    @Nullable
    @Override
    public TransferResult transfer(ItemStack stack, FluidStack fluid, IFluidHandler handler) {
        int amount = this.fluid.getAmount(fluid.getFluid());
        FluidStack toDrain = new FluidStack(fluid, amount);
        FluidStack simulated = handler.drain(toDrain.copy(), FluidAction.SIMULATE);
        if (simulated.getAmount() == amount) {
            FluidStack actual = handler.drain(toDrain.copy(), FluidAction.EXECUTE);
            if (actual.getAmount() != amount) {
                Mantle.logger.error("Wrong amount drained from {}, expected {}, filled {}", Registries.ITEM.getId(stack.getItem()), fluid.getAmount(), actual.getAmount());
            }
            return new TransferResult(this.getFilled(toDrain), toDrain, true);
        }
        return null;
    }

    @Override
    public JsonObject serialize(JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.add("input", this.input.toJson());
        json.add("filled", this.filled.serialize(false));
        json.add("fluid", this.fluid.serialize());
        return json;
    }

    /**
     * Unique loader instance
     */
    public static final JsonDeserializer<FillFluidContainerTransfer> DESERIALIZER = new Deserializer<>(FillFluidContainerTransfer::new);

    public record Deserializer<T extends FillFluidContainerTransfer>(
            TriFunction<Ingredient, ItemOutput, FluidIngredient, T> factory) implements JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            Ingredient input = Ingredient.fromJson(JsonHelper.getElement(json, "input"));
            ItemOutput filled = ItemOutput.Loadable.REQUIRED_ITEM.getIfPresent(json, "filled");
            FluidIngredient fluid = FluidIngredient.deserialize(json, "fluid");
            return this.factory.apply(input, filled, fluid);
        }
    }
}
