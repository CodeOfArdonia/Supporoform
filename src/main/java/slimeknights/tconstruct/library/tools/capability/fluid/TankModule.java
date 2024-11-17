package slimeknights.tconstruct.library.tools.capability.fluid;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolFluidCapability.FluidModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Standard implementation of a tank module using the {@link ToolTankHelper}.
 * Feel free to request abstracting out an aspect of it if you wish to have less duplication in a non-standard implementation.
 * Unregistered as modifiers have no way to register new tool stats.
 */
@SuppressWarnings("ClassCanBeRecord")  // Want to leave extendable
@RequiredArgsConstructor
public class TankModule implements HookProvider, FluidModifierHook, TooltipModifierHook, VolatileDataModifierHook, ValidateModifierHook, ModifierRemovalHook {
    private static final String FLUID_KEY = ToolTankHelper.CAPACITY_STAT.getTranslationKey() + ".fluid";
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<TankModule>defaultHooks(ToolFluidCapability.HOOK, ModifierHooks.TOOLTIP, ModifierHooks.VOLATILE_DATA, ModifierHooks.VALIDATE, ModifierHooks.REMOVE);


    /**
     * Helper handling updating fluids
     */
    private final ToolTankHelper helper;


    /* Module logic */

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ModDataNBT volatileData) {
        ToolFluidCapability.addTanks(modifier, volatileData, this);
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        FluidStack current = this.helper.getFluid(tool);
        if (!current.isEmpty()) {
            tooltip.add(Text.translatable(FLUID_KEY)
                    .append(Text.translatable(TankCapacityStat.MB_FORMAT, Util.COMMA_FORMAT.format(current.getAmount()))
                            .append(" ")
                            .append(current.getDisplayName())
                            .withStyle(style -> style.withColor(ToolTankHelper.CAPACITY_STAT.getColor()))));
        }
        tooltip.add(this.helper.getCapacityStat().formatValue(this.helper.getCapacity(tool)));
    }

    @Override
    public int getTankCapacity(IToolStackView tool, ModifierEntry modifier, int tank) {
        return this.helper.getCapacity(tool);
    }

    @Override
    public FluidStack getFluidInTank(IToolStackView tool, ModifierEntry modifier, int tank) {
        return this.helper.getFluid(tool);
    }


    /* Cleanup */

    @Nullable
    @Override
    public Text validate(IToolStackView tool, ModifierEntry modifier) {
        FluidStack fluid = this.helper.getFluid(tool);
        int capacity = this.helper.getCapacity(tool);
        if (fluid.getAmount() > capacity) {
            fluid.setAmount(capacity);
            this.helper.setFluid(tool, fluid);
        }
        return null;
    }

    @Nullable
    @Override
    public Text onRemoved(IToolStackView tool, Modifier modifier) {
        this.helper.setFluid(tool, FluidStack.EMPTY);
        return null;
    }


    /* Filling and draining */

    @Override
    public int fill(IToolStackView tool, ModifierEntry modifier, FluidStack resource, FluidAction action) {
        // make sure this modifier is in charge of the tank, that is first come first serve
        modifier.getId();
        if (!resource.isEmpty()) {
            // if empty, just directly fill, setFluid will check capacity
            FluidStack current = this.helper.getFluid(tool);
            int capacity = this.helper.getCapacity(tool);
            if (current.isEmpty()) {
                if (action.execute()) {
                    this.helper.setFluid(tool, resource);
                }
                return Math.min(resource.getAmount(), capacity);
            }
            // if the fluid matches and we have space, update
            if (current.getAmount() < capacity && current.isFluidEqual(resource)) {
                int filled = Math.min(resource.getAmount(), capacity - current.getAmount());
                if (filled > 0 && action.execute()) {
                    current.grow(filled);
                    this.helper.setFluid(tool, current);
                }
                return filled;
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, FluidStack resource, FluidAction action) {
        modifier.getId();
        if (!resource.isEmpty()) {
            // ensure we have something and it matches the request
            FluidStack current = this.helper.getFluid(tool);
            if (!current.isEmpty() && current.isFluidEqual(resource)) {
                // create the drained stack
                FluidStack drained = new FluidStack(current, Math.min(current.getAmount(), resource.getAmount()));
                // if executing, removing it
                if (action.execute()) {
                    if (drained.getAmount() == current.getAmount()) {
                        this.helper.setFluid(tool, FluidStack.EMPTY);
                    } else {
                        current.shrink(drained.getAmount());
                        this.helper.setFluid(tool, current);
                    }
                }
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(IToolStackView tool, ModifierEntry modifier, int maxDrain, FluidAction action) {
        modifier.getId();
        if (maxDrain > 0) {
            // ensure we have something and it matches the request
            FluidStack current = this.helper.getFluid(tool);
            if (!current.isEmpty()) {
                // create the drained stack
                FluidStack drained = new FluidStack(current, Math.min(current.getAmount(), maxDrain));
                // if executing, removing it
                if (action.execute()) {
                    if (drained.getAmount() == current.getAmount()) {
                        this.helper.setFluid(tool, FluidStack.EMPTY);
                    } else {
                        current.shrink(drained.getAmount());
                        this.helper.setFluid(tool, current);
                    }
                }
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }
}
