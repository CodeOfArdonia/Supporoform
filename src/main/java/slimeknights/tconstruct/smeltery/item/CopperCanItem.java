package slimeknights.tconstruct.smeltery.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.recipe.FluidValues;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fluid container holding 1 ingot of fluid
 */
public class CopperCanItem extends Item {
    private static final String TAG_FLUID = "fluid";
    private static final String TAG_FLUID_TAG = "fluid_tag";

    public CopperCanItem(Settings properties) {
        super(properties);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NbtCompound nbt) {
        return new CopperCanFluidHandler(stack);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return getFluid(stack) != Fluids.EMPTY;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (hasCraftingRemainingItem(stack)) {
            return new ItemStack(this);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        Fluid fluid = getFluid(stack);
        if (fluid != Fluids.EMPTY) {
            NbtCompound fluidTag = getFluidTag(stack);
            MutableText text;
            if (fluidTag != null) {
                FluidStack displayFluid = new FluidStack(fluid, FluidValues.INGOT, fluidTag);
                text = displayFluid.getDisplayName().plainCopy();
            } else {
                text = Text.translatable(fluid.getFluidType().getDescriptionId());
            }
            tooltip.add(Text.translatable(this.getTranslationKey() + ".contents", text).formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable(this.getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
        }
    }

    /**
     * Sets the fluid on the given stack
     */
    public static ItemStack setFluid(ItemStack stack, FluidStack fluid) {
        // if empty, try to remove the NBT, helps with recipes
        if (fluid.isEmpty()) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(TAG_FLUID);
                nbt.remove(TAG_FLUID_TAG);
                if (nbt.isEmpty()) {
                    stack.setNbt(null);
                }
            }
        } else {
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putString(TAG_FLUID, Registry.FLUID.getKey(fluid.getFluid()).toString());
            NbtCompound fluidTag = fluid.getTag();
            if (fluidTag != null) {
                nbt.put(TAG_FLUID_TAG, fluidTag.copy());
            } else {
                nbt.remove(TAG_FLUID_TAG);
            }
        }
        return stack;
    }

    /**
     * Gets the fluid from the given stack
     */
    public static Fluid getFluid(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            Identifier location = Identifier.tryParse(nbt.getString(TAG_FLUID));
            if (location != null && ForgeRegistries.FLUIDS.containsKey(location)) {
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(location);
                if (fluid != null) {
                    return fluid;
                }
            }
        }
        return Fluids.EMPTY;
    }

    /**
     * Gets the fluid NBT from the given stack
     */
    @Nullable
    public static NbtCompound getFluidTag(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(TAG_FLUID_TAG, NbtElement.COMPOUND_TYPE)) {
            return nbt.getCompound(TAG_FLUID_TAG);
        }
        return null;
    }

    /**
     * Gets a string variant name for the given stack
     *
     * @param stack Stack instance to check
     * @return String variant name
     */
    public static String getSubtype(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            return nbt.getString(TAG_FLUID);
        }
        return "";
    }
}
