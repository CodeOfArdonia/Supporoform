package slimeknights.tconstruct.library.recipe.melting;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.data.loadable.common.FluidStackLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recipe to melt all castable tool parts of a given material
 */
public class MaterialMeltingRecipe implements IMeltingRecipe, IMultiRecipe<MeltingRecipe> {
    public static final RecordLoadable<MaterialMeltingRecipe> LOADER = RecordLoadable.create(
            ContextKey.ID.requiredField(),
            MaterialVariantId.LOADABLE.requiredField("input", r -> r.input.getVariant()),
            IntLoadable.FROM_ONE.requiredField("temperature", r -> r.temperature),
            FluidStackLoadable.REQUIRED_STACK.requiredField("result", r -> r.result),
            MaterialMeltingRecipe::new);

    @Getter
    private final Identifier id;
    private final MaterialVariant input;
    private final int temperature;
    private final FluidStack result;

    public MaterialMeltingRecipe(Identifier id, MaterialVariantId input, int temperature, FluidStack result) {
        this.id = id;
        this.input = MaterialVariant.of(input);
        this.temperature = temperature;
        this.result = result;
    }

    @Override
    public boolean matches(IMeltingContainer inv, World worldIn) {
        if (this.input.isUnknown()) {
            return false;
        }
        ItemStack stack = inv.getStack();
        if (stack.isEmpty() || MaterialCastingLookup.getItemCost(stack.getItem()) == 0) {
            return false;
        }
        return this.input.matchesVariant(stack);
    }

    @Override
    public int getTemperature(IMeltingContainer inv) {
        return this.temperature;
    }

    @Override
    public int getTime(IMeltingContainer inv) {
        int cost = MaterialCastingLookup.getItemCost(inv.getStack().getItem());
        return IMeltingRecipe.calcTimeForAmount(this.temperature, this.result.getAmount() * cost);
    }

    @Override
    public FluidStack getOutput(IMeltingContainer inv) {
        int cost = MaterialCastingLookup.getItemCost(inv.getStack().getItem());
        return new FluidStack(this.result, this.result.getAmount() * cost);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TinkerSmeltery.materialMeltingSerializer.get();
    }


    /* JEI display */
    private List<MeltingRecipe> multiRecipes = null;

    @Override
    public List<MeltingRecipe> getRecipes() {
        if (this.multiRecipes == null) {
            if (this.input.get().isHidden()) {
                this.multiRecipes = Collections.emptyList();
            } else {
                // 1 recipe for each part
                MaterialId inputId = this.input.getId();
                this.multiRecipes = MaterialCastingLookup
                        .getAllItemCosts().stream()
                        .filter(entry -> entry.getKey().canUseMaterial(inputId))
                        .map(entry -> {
                            FluidStack output = this.result;
                            if (entry.getIntValue() != 1) {
                                output = new FluidStack(output, output.getAmount() * entry.getIntValue());
                            }
                            return new MeltingRecipe(this.id, "", MaterialIngredient.of(entry.getKey(), inputId), output, this.temperature,
                                    IMeltingRecipe.calcTimeForAmount(this.temperature, output.getAmount()), Collections.emptyList());
                        }).collect(Collectors.toList());
            }
        }
        return this.multiRecipes;
    }
}
