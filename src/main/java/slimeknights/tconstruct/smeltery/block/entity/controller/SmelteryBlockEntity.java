package slimeknights.tconstruct.smeltery.block.entity.controller;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags.Items;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.MultiAlloyingModule;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.SmelteryAlloyTank;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.SmelteryMultiblock;

import org.jetbrains.annotations.Nullable;

public class SmelteryBlockEntity extends HeatingStructureBlockEntity {
    /**
     * Fluid capacity per internal block
     */
    private static final int CAPACITY_PER_BLOCK = FluidValues.INGOT * 12;
    /**
     * Number of wall blocks needed to increase the fuel cost by 1
     */
    private static final int BLOCKS_PER_FUEL = 15;
    /**
     * Name of the UI
     */
    private static final Text NAME = TConstruct.makeTranslation("gui", "smeltery");

    /**
     * Module handling alloys
     */
    private final SmelteryAlloyTank alloyTank = new SmelteryAlloyTank(this.tank);
    @Getter
    private final MultiAlloyingModule alloyingModule = new MultiAlloyingModule(this, this.alloyTank);

    public SmelteryBlockEntity(BlockPos pos, BlockState state) {
        super(TinkerSmeltery.smeltery.get(), pos, state, NAME);
    }

    @Override
    protected HeatingStructureMultiblock<?> createMultiblock() {
        return new SmelteryMultiblock(this);
    }

    @Override
    protected MeltingModuleInventory createMeltingInventory() {
        return new MeltingModuleInventory(this, this.tank, Config.COMMON.smelteryOreRate);
    }

    @Override
    protected boolean isDebugItem(ItemStack stack) {
        return stack.isIn(Items.SMELTERY_DEBUG);
    }

    @Override
    protected void heat() {
        if (this.structure == null || this.world == null) {
            return;
        }

        // the next set of behaviors all require fuel, skip if no tanks
        if (this.structure.hasTanks()) {
            // every second, interact with entities, will consume fuel if needed
            boolean entityMelted = false;
            if (this.tick == 12) {
                entityMelted = this.entityModule.interactWithEntities();
            }
            // run in four phases alternating each tick, so each thing runs once every 4 ticks
            switch (this.tick % 4) {
                // first tick, find fuel if needed
                case 0:
                    if (!this.fuelModule.hasFuel()) {
                        // if we melted something already, we need fuel
                        if (entityMelted) {
                            this.fuelModule.findFuel(true);
                        } else {
                            // both alloying and melting need to know the temperature
                            int possibleTemp = this.fuelModule.findFuel(false);
                            this.alloyTank.setTemperature(possibleTemp);
                            if (this.meltingInventory.canHeat(possibleTemp) || this.alloyingModule.canAlloy()) {
                                this.fuelModule.findFuel(true);
                            }
                        }
                    }
                    break;
                // second tick: melt items
                case 1:
                    if (this.fuelModule.hasFuel()) {
                        this.meltingInventory.heatItems(this.fuelModule.getTemperature());
                    } else {
                        this.meltingInventory.coolItems();
                    }
                    break;
                // third tick: alloy alloys
                case 2:
                    if (this.fuelModule.hasFuel()) {
                        this.alloyTank.setTemperature(this.fuelModule.getTemperature());
                        this.alloyingModule.doAlloy();
                    }
                    break;
                // fourth tick: consume fuel, update fluids
                case 3: {
                    // update the active state
                    boolean hasFuel = this.fuelModule.hasFuel();
                    BlockState state = this.getCachedState();
                    if (state.get(ControllerBlock.ACTIVE) != hasFuel) {
                        this.world.setBlockState(this.pos, state.with(ControllerBlock.ACTIVE, hasFuel));
                    }
                    this.fuelModule.decreaseFuel(this.fuelRate);
                    break;
                }
            }
        }
    }

    @Override
    protected void setStructure(@Nullable StructureData structure) {
        super.setStructure(structure);
        if (structure != null) {
            int dx = structure.getInnerX(), dy = structure.getInnerY(), dz = structure.getInnerZ();
            int size = dx * dy * dz;
            this.tank.setCapacity(CAPACITY_PER_BLOCK * size);
            this.meltingInventory.resize(size, this.dropItem);
            // fuel rate: every 15 blocks in the wall makes the fuel cost 1 more
            // perimeter: 2 of the X and the Z wall, one of the floor
            this.fuelRate = 1 + ((2 * (dx * dy) + 2 * (dy * dz) + (dx * dz))) / BLOCKS_PER_FUEL;
        }
    }

    @Override
    public void notifyFluidsChanged(FluidChange type, FluidStack fluid) {
        super.notifyFluidsChanged(type, fluid);

        // adding a new fluid means recipes that previously did not match might match now
        // can ignore removing a fluid as that is handled internally by the module
        if (type == FluidChange.ADDED) {
            this.alloyingModule.clearCachedRecipes();
        }
    }
}
