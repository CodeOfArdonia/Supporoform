package slimeknights.tconstruct.library.recipe.melting;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.library.json.field.MergingField;
import slimeknights.tconstruct.library.json.field.MergingField.MissingMode;
import slimeknights.tconstruct.library.json.field.MergingListField;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.List;
import java.util.function.Function;

/**
 * Melting recipe that scale output based on input damage
 */
public class DamageableMeltingRecipe extends MeltingRecipe {
    /**
     * Loader instance
     */
    public static final RecordLoadable<DamageableMeltingRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP, INPUT, OUTPUT, TEMPERATURE, TIME, BYPRODUCTS,
            new MergingField<>(IntLoadable.FROM_ONE.defaultField("unit_size", 1, r -> r.unitSize), "result", MissingMode.IGNORE),
            new MergingListField<>(IntLoadable.FROM_ONE.defaultField("unit_size", 1, Function.identity()), "byproducts", r -> r.byproductSizes),
            DamageableMeltingRecipe::new);

    /**
     * Sizes of each unit in the recipe. Index 0 is the main output, 1 and onwards is secondary outputs
     */
    private final int unitSize;
    /**
     * Sizes of byproducts
     */
    private final List<Integer> byproductSizes;

    public DamageableMeltingRecipe(Identifier id, String group, Ingredient input, FluidStack output, int temperature, int time, List<FluidStack> byproducts, int unitSize, List<Integer> byproductSizes) {
        super(id, group, input, output, temperature, time, byproducts);
        this.unitSize = unitSize;
        this.byproductSizes = byproductSizes;
    }

    /**
     * Scales a fluid stack based on the damage
     */
    private static FluidStack scaleOutput(FluidStack fluid, int damage, int maxDamage, int unitSize) {
        long amount = fluid.getAmount() * (maxDamage - damage) / maxDamage;
        // mimimum output is one unit
        if (amount <= unitSize) {
            amount = Math.max(unitSize, 1);
        } else if (unitSize > 1) {
            // round down to the nearest unit
            long remainder = amount % unitSize;
            if (remainder > 0) {
                amount -= remainder;
            }
        }
        return new FluidStack(fluid, amount);
    }

    @Override
    public FluidStack getOutput(IMeltingContainer inv) {
        FluidStack output = this.getOutput();
        ItemStack input = inv.getStack();
        int maxDamage = input.getMaxDamage();
        if (maxDamage <= 0) {
            return output.copy();
        }
        // scale output based on damage value, its possible 1mb is a lot for some high durability things, but whatever
        return scaleOutput(output, input.getDamage(), maxDamage, this.unitSize);
    }

    @Override
    public void handleByproducts(IMeltingContainer inv, IFluidHandler handler) {
        ItemStack input = inv.getStack();
        int maxDamage = input.getMaxDamage();
        if (maxDamage <= 0) {
            super.handleByproducts(inv, handler);
        } else {
            // fill byproducts until we run out of space or byproducts
            int itemDamage = input.getDamage();
            for (int i = 0; i < this.byproducts.size(); i++) {
                FluidStack fluidStack = this.byproducts.get(i);
                handler.fill(scaleOutput(fluidStack, itemDamage, maxDamage, i < this.byproductSizes.size() ? this.byproductSizes.get(i) : this.unitSize), FluidAction.EXECUTE);
            }
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerSmeltery.damagableMeltingSerializer.get();
    }
}
