package slimeknights.tconstruct.smeltery.block.entity.controller;

import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags.Items;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.module.ByproductMeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.FoundryMultiblock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.HeatingStructureMultiblock.StructureData;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class FoundryBlockEntity extends HeatingStructureBlockEntity {
    /**
     * Fluid capacity per internal block
     */
    private static final int CAPACITY_PER_BLOCK = FluidValues.INGOT * 8;
    /**
     * GUI name
     */
    private static final Text NAME = TConstruct.makeTranslation("gui", "foundry");
    /**
     * Number of wall blocks needed to increase the fuel cost by 1
     * this is a bit higher than the smeltery as the structure uses more blocks, balances out in larger structures
     */
    private static final int BLOCKS_PER_FUEL = 18;

    public FoundryBlockEntity(BlockPos pos, BlockState state) {
        super(TinkerSmeltery.foundry.get(), pos, state, NAME);
    }

    @Override
    protected HeatingStructureMultiblock<?> createMultiblock() {
        return new FoundryMultiblock(this);
    }


    @Override
    protected MeltingModuleInventory createMeltingInventory() {
        return new ByproductMeltingModuleInventory(this, this.tank, Config.COMMON.foundryOreRate);
    }

    @Override
    protected boolean isDebugItem(ItemStack stack) {
        return stack.isIn(Items.FOUNDRY_DEBUG);
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
                            if (this.meltingInventory.canHeat(this.fuelModule.findFuel(false))) {
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
            // tank capacity includes walls and floor
            this.tank.setCapacity(CAPACITY_PER_BLOCK * (dx + 2) * (dy + 1) * (dz + 2));
            // item capacity uses just inner space
            this.meltingInventory.resize(dx * dy * dz, this.dropItem);
            // fuel rate: every 20 blocks in the wall makes the fuel cost 1 more
            // perimeter: to prevent double counting, frame just added on X and floor
            this.fuelRate = 1 + (2 * ((dx + 2) * dy) + 2 * (dy * dz) + ((dx + 2) * (dz + 2))) / BLOCKS_PER_FUEL;
        }
    }
}
