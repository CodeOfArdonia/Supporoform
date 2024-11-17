package slimeknights.mantle.registration.adapter;

import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.*;
import net.minecraft.block.PressurePlateBlock.ActivationRule;
import net.minecraft.block.enums.Instrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import slimeknights.mantle.block.MantleStandingSignBlock;
import slimeknights.mantle.block.MantleWallSignBlock;
import slimeknights.mantle.block.StrippableLogBlock;
import slimeknights.mantle.block.entity.MantleSignBlockEntity;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.WallBuildingBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject.WoodVariant;

import java.util.function.Function;
import java.util.function.Supplier;

import static slimeknights.mantle.util.RegistryHelper.getHolder;

/**
 * Provides utility registration methods when registering blocks.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BlockRegistryAdapter extends EnumRegistryAdapter<Block> {

    /**
     * @inheritDoc
     */
    public BlockRegistryAdapter(Registry<Block> registry) {
        super(registry);
    }

    /**
     * @inheritDoc
     */
    public BlockRegistryAdapter(Registry<Block> registry, String modid) {
        super(registry, modid);
    }

    /**
     * Registers a block override based on the given block
     *
     * @param constructor Override constructor
     * @param base        Base block
     * @param <T>         Block type
     * @return Registered block
     */
    public <T extends Block> T registerOverride(Function<Settings, T> constructor, Block base) {
        return this.register(constructor.apply(Settings.copy(base)), base);
    }

    /* Building */

    /**
     * Registers the given block as well as a slab and a stair variant for it.
     * Uses the vanilla slab and stair blocks. Uses the passed blocks properties for both.
     * Slabs and stairs are registered with a "_slab" and "_stairs" prefix
     *
     * @param block The main block to register and whose properties to use
     * @param name  The registry name to use for the block and as base for the slab and stairs
     * @return BuildingBlockObject for the given block
     */
    public BuildingBlockObject registerBuilding(Block block, String name) {
        return new BuildingBlockObject(
                this.register(block, name),
                this.register(new SlabBlock(Settings.copy(block)), name + "_slab"),
                this.register(new StairsBlock(block.getDefaultState(), Settings.copy(block)), name + "_stairs")
        );
    }

    /**
     * Same as {@link #registerBuilding(Block, String)}, but also includes a wall variant
     *
     * @param block The main block to register and whose properties to use
     * @param name  The registry name to use for the block and as base for the slab and stairs
     * @return BuildingBlockObject for the given block
     */
    public WallBuildingBlockObject registerWallBuilding(Block block, String name) {
        return new WallBuildingBlockObject(
                this.registerBuilding(block, name),
                this.register(new WallBlock(Settings.copy(block)), name + "_wall")
        );
    }

    /**
     * Same as {@link #registerBuilding(Block, String)}, but also includes a fence variant
     *
     * @param block The main block to register and whose properties to use
     * @param name  The registry name to use for the block and as base for the slab and stairs
     * @return BuildingBlockObject for the given block
     */
    public FenceBuildingBlockObject registerFenceBuilding(Block block, String name) {
        return new FenceBuildingBlockObject(
                this.registerBuilding(block, name),
                this.register(new FenceBlock(Settings.copy(block)), name + "_fence")
        );
    }


    /**
     * Registers a new wood object
     *
     * @param name            Name of the wood object
     * @param behaviorCreator Logic to create the behavior
     * @return Wood object
     */
    public WoodBlockObject registerWood(String name, Function<WoodVariant, Settings> behaviorCreator) {
        BlockSetType setType = new BlockSetType(this.resourceName(name));
        WoodType woodType = new WoodType(this.resourceName(name), setType);
        BlockSetType.register(setType);
        WoodType.register(woodType);
        RegistrationHelper.registerWoodType(woodType);

        // planks
        Settings planksProps = behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).strength(2.0f, 3.0f);
        BuildingBlockObject planks = this.registerBuilding(new Block(planksProps), name + "_planks");
        FenceBlock fence = this.register(new FenceBlock(Settings.copy(planks.get()).solid()), name + "_fence");
        // logs and wood
        Supplier<? extends PillarBlock> stripped = () -> new PillarBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).strength(2.0f));
        PillarBlock strippedLog = this.register(stripped.get(), "stripped_" + name + "_log");
        PillarBlock strippedWood = this.register(stripped.get(), "stripped_" + name + "_wood");
        PillarBlock log = this.register(new StrippableLogBlock(getHolder(Registries.BLOCK, strippedLog), behaviorCreator.apply(WoodVariant.LOG).instrument(Instrument.BASS).strength(2.0f)), name + "_log");
        PillarBlock wood = this.register(new StrippableLogBlock(getHolder(Registries.BLOCK, strippedWood), behaviorCreator.apply(WoodVariant.WOOD).instrument(Instrument.BASS).strength(2.0f)), name + "_wood");

        // doors
        DoorBlock door = this.register(new DoorBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).strength(3.0F).nonOpaque().pistonBehavior(PistonBehavior.DESTROY), setType), name + "_door");
        TrapdoorBlock trapdoor = this.register(new TrapdoorBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).strength(3.0F).nonOpaque().allowsSpawning(Blocks::never), setType), name + "_trapdoor");
        FenceGateBlock fenceGate = this.register(new FenceGateBlock(Settings.copy(fence), woodType), name + "_fence_gate");
        // redstone
        Settings redstoneProps = behaviorCreator.apply(WoodVariant.PLANKS).solid().instrument(Instrument.BASS).noCollision().pistonBehavior(PistonBehavior.DESTROY).strength(0.5F);
        PressurePlateBlock pressurePlate = this.register(new PressurePlateBlock(ActivationRule.EVERYTHING, redstoneProps, setType), name + "_pressure_plate");
        ButtonBlock button = this.register(new ButtonBlock(redstoneProps, setType, 30, true), name + "_button");
        // signs
        SignBlock standingSign = this.register(new MantleStandingSignBlock(behaviorCreator.apply(WoodVariant.PLANKS).solid().instrument(Instrument.BASS).noCollision().strength(1.0F), woodType), name + "_sign");
        WallSignBlock wallSign = this.register(new MantleWallSignBlock(behaviorCreator.apply(WoodVariant.PLANKS).solid().instrument(Instrument.BASS).noCollision().strength(1.0F).dropsLike(standingSign), woodType), name + "_wall_sign");
        // tell mantle to inject these into the TE
        MantleSignBlockEntity.registerSignBlock(() -> standingSign);
        MantleSignBlockEntity.registerSignBlock(() -> wallSign);
        // finally, return
        return new WoodBlockObject(this.getResource(name), woodType, planks, log, strippedLog, wood, strippedWood, fence, fenceGate, door, trapdoor, pressurePlate, button, standingSign, wallSign);
    }

    /* Fluid */

    /**
     * Registers a fluid block from a fluid
     *
     * @param fluid      Fluid supplier
     * @param color      Fluid color
     * @param lightLevel Fluid light level
     * @param name       Fluid name, unfortunately no way to fetch from the fluid as it does not exist yet
     * @return Fluid block instance
     */
    public FluidBlock registerFluidBlock(Supplier<? extends ForgeFlowingFluid> fluid, MapColor color, int lightLevel, String name) {
        return this.register(
                new FluidBlock(fluid, Settings.create().mapColor(color).noCollision().strength(100.0F).dropsNothing().luminance((state) -> lightLevel)),
                name + "_fluid");
    }
}
