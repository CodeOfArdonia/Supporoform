package slimeknights.mantle.loot.function;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.util.JsonHelper;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.loot.MantleLoot;
import slimeknights.mantle.recipe.helper.RecipeHelper;

/**
 * Loot function to set the fluid on a dropped item
 */
public class SetFluidLootFunction extends ConditionalLootFunction {
    public static final Serializer SERIALIZER = new Serializer();

    /**
     * Fluid to add to the item
     */
    private final FluidStack fluid;

    protected SetFluidLootFunction(LootCondition[] conditionsIn, FluidStack fluid) {
        super(conditionsIn);
        this.fluid = fluid;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> {
                    handler.fill(this.fluid.copy(), FluidAction.EXECUTE);
                    return handler.getContainer();
                }).orElse(stack);
    }

    @Override
    public LootFunctionType getType() {
        return MantleLoot.SET_FLUID_FUNCTION;
    }

    /**
     * Creates a new builder with the given fluid
     *
     * @param fluid Fluid to set
     * @return Builder instance
     */
    public static net.minecraft.loot.function.ConditionalLootFunction.Builder<?> builder(FluidStack fluid) {
        return builder(conditions -> new SetFluidLootFunction(conditions, fluid));
    }

    /**
     * Serializer logic for the function
     */
    private static class Serializer extends ConditionalLootFunction.Serializer<SetFluidLootFunction> {
        @Override
        public void toJson(JsonObject json, SetFluidLootFunction loot, JsonSerializationContext context) {
            super.toJson(json, loot, context);
            json.add("fluid", RecipeHelper.serializeFluidStack(loot.fluid));
        }

        @Override
        public SetFluidLootFunction fromJson(JsonObject object, JsonDeserializationContext context, LootCondition[] conditions) {
            FluidStack fluid = RecipeHelper.deserializeFluidStack(JsonHelper.getObject(object, "fluid"));
            return new SetFluidLootFunction(conditions, fluid);
        }
    }
}
