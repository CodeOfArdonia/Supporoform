package slimeknights.tconstruct.library.recipe.casting.container;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;

import java.util.Collections;
import java.util.List;

/**
 * Casting recipe that takes an arbitrary fluid for a given amount and fills a container
 */
@RequiredArgsConstructor
public class ContainerFillingRecipe implements ICastingRecipe, IMultiRecipe<DisplayCastingRecipe> {
    public static final RecordLoadable<ContainerFillingRecipe> LOADER = RecordLoadable.create(
            LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
            IntLoadable.FROM_ONE.requiredField("fluid_amount", r -> r.fluidAmount),
            Loadables.ITEM.requiredField("container", r -> r.container),
            ContainerFillingRecipe::new);

    @Getter
    private final TypeAwareRecipeSerializer<?> serializer;
    @Getter
    private final Identifier id;
    @Getter
    private final String group;
    private final int fluidAmount;
    private final Item container;

    @Override
    public RecipeType<?> getType() {
        return serializer.getType();
    }

    @Override
    public long getFluidAmount(ICastingContainer inv) {
        Fluid fluid = inv.getFluid();
        return inv.getStack().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> handler.fill(new FluidStack(fluid, this.fluidAmount), FluidAction.SIMULATE))
                .orElse(0);
    }

    @Override
    public boolean isConsumed() {
        return true;
    }

    @Override
    public boolean switchSlots() {
        return false;
    }

    @Override
    public int getCoolingTime(ICastingContainer inv) {
        return 5;
    }

    @Override
    public boolean matches(ICastingContainer inv, World worldIn) {
        ItemStack stack = inv.getStack();
        Fluid fluid = inv.getFluid();
        return stack.getItem() == this.container.asItem()
                && stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .filter(handler -> handler.fill(new FluidStack(fluid, this.fluidAmount), FluidAction.SIMULATE) > 0)
                .isPresent();
    }

    /**
     * @deprecated use {@link ICastingRecipe#craft(Inventory)}
     */
    @Override
    @Deprecated
    public ItemStack getOutput(DynamicRegistryManager manager) {
        return new ItemStack(this.container);
    }

    @Override
    public ItemStack assemble(ICastingContainer inv) {
        ItemStack stack = inv.getStack().copy();
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
            handler.fill(new FluidStack(inv.getFluid(), this.fluidAmount, inv.getFluidTag()), FluidAction.EXECUTE);
            return handler.getContainer();
        }).orElse(stack);
    }

    /* Display */
    /**
     * Cache of items to display for this container
     */
    private List<DisplayCastingRecipe> displayRecipes = null;

    @Override
    public List<DisplayCastingRecipe> getRecipes() {
        if (displayRecipes == null) {
            List<ItemStack> casts = Collections.singletonList(new ItemStack(container));
            displayRecipes = Registries.FLUID.stream()
                    .filter(fluid -> fluid.getBucketItem() != Items.AIR && fluid.isStill(fluid.getDefaultState()))
                    .map(fluid -> {
                        FluidStack fluidStack = new FluidStack(fluid, fluidAmount);
                        ItemStack stack = new ItemStack(container);
                        stack = FluidUtil.getFluidHandler(stack).map(handler -> {
                            handler.fill(fluidStack, FluidAction.EXECUTE);
                            return handler.getContainer();
                        }).orElse(stack);
                        return new DisplayCastingRecipe(getType(), casts, Collections.singletonList(fluidStack), stack, 5, true);
                    })
                    .toList();
        }
        return this.displayRecipes;
    }
}
