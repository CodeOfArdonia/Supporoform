package slimeknights.tconstruct.fluids.item;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import slimeknights.mantle.fluid.transfer.EmptyFluidWithNBTTransfer;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.tconstruct.TConstruct;

/**
 * Fluid transfer info that empties a fluid from an item, copying the fluid's NBT to the stack
 */
public class EmptyPotionTransfer extends EmptyFluidWithNBTTransfer implements IFluidContainerTransfer {
    public static final Identifier ID = TConstruct.getResource("empty_potion");

    public EmptyPotionTransfer(Ingredient input, ItemOutput filled, FluidStack fluid) {
        super(input, filled, fluid);
    }

    @Override
    protected FluidStack getFluid(ItemStack stack) {
        if (PotionUtil.getPotion(stack) == Potions.WATER) {
            return new FluidStack(Fluids.WATER, this.fluid.getAmount());
        }
        return new FluidStack(this.fluid.getFluid(), this.fluid.getAmount(), stack.getNbt());
    }

    @Override
    public JsonObject serialize(JsonSerializationContext context) {
        JsonObject json = super.serialize(context);
        json.addProperty("type", ID.toString());
        return json;
    }

    /**
     * Unique loader instance
     */
    public static final JsonDeserializer<EmptyPotionTransfer> DESERIALIZER = new Deserializer<>(EmptyPotionTransfer::new);
}
