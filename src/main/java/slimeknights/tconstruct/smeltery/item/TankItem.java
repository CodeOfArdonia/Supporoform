package slimeknights.tconstruct.smeltery.item;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TankItem extends BlockTooltipItem {
    private static final String KEY_FLUID = TConstruct.makeTranslationKey("block", "tank.fluid");
    private static final String KEY_MB = TConstruct.makeTranslationKey("block", "tank.mb");
    private static final String KEY_INGOTS = TConstruct.makeTranslationKey("block", "tank.ingots");
    private static final String KEY_MIXED = TConstruct.makeTranslationKey("block", "tank.mixed");

    private final boolean limitStackSize;

    public TankItem(Block blockIn, Settings builder, boolean limitStackSize) {
        super(blockIn, builder);
        this.limitStackSize = limitStackSize;
    }

    /**
     * Checks if the tank item is filled
     */
    private static boolean isFilled(ItemStack stack) {
        // has a container if not empty
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(NBTTags.TANK, NbtElement.COMPOUND_TYPE);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return isFilled(stack);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return isFilled(stack) ? new ItemStack(this) : ItemStack.EMPTY;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (!this.limitStackSize) {
            return super.getMaxCount(stack);
        }
        return isFilled(stack) ? 16 : 64;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        if (stack.hasNbt()) {
            FluidTank tank = getFluidTank(stack);
            if (tank.getFluidAmount() > 0) {
                // TODO: migrate to a fluid tooltip JSON?
                tooltip.add(Text.translatable(KEY_FLUID, tank.getFluid().getDisplayName()).formatted(Formatting.GRAY));
                long amount = tank.getFluidAmount();
                TooltipKey key = SafeClientAccess.getTooltipKey();
                if (tank.getCapacity() % FluidValues.INGOT != 0 || key == TooltipKey.SHIFT) {
                    tooltip.add(Text.translatable(KEY_MB, amount).formatted(Formatting.GRAY));
                } else {
                    long ingots = amount / FluidValues.INGOT;
                    long mb = amount % FluidValues.INGOT;
                    if (mb == 0) {
                        tooltip.add(Text.translatable(KEY_INGOTS, ingots).formatted(Formatting.GRAY));
                    } else {
                        tooltip.add(Text.translatable(KEY_MIXED, ingots, mb).formatted(Formatting.GRAY));
                    }
                    if (key != TooltipKey.UNKNOWN) {
                        tooltip.add(FluidTooltipHandler.HOLD_SHIFT);
                    }
                }

            }
        } else {
            super.appendTooltip(stack, worldIn, tooltip, flagIn);
        }
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NbtCompound nbt) {
        return new TankItemFluidHandler(stack);
    }

    /**
     * Sets the tank to the given stack
     *
     * @param stack Stack
     * @param tank  Tank instance
     * @return Stack with tank
     */
    public static ItemStack setTank(ItemStack stack, FluidTank tank) {
        if (tank.isEmpty()) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBTTags.TANK);
                if (nbt.isEmpty()) {
                    stack.setNbt(null);
                }
            }
        } else {
            stack.getOrCreateNbt().put(NBTTags.TANK, tank.writeToNBT(new NbtCompound()));
        }
        return stack;
    }

    /**
     * Gets the tank for the given stack
     *
     * @param stack Tank stack
     * @return Tank stored in the stack
     */
    public static FluidTank getFluidTank(ItemStack stack) {
        FluidTank tank = new FluidTank(TankBlockEntity.getCapacity(stack.getItem()));
        if (stack.hasNbt()) {
            assert stack.getNbt() != null;
            tank.readFromNBT(stack.getNbt().getCompound(NBTTags.TANK));
        }
        return tank;
    }
}
