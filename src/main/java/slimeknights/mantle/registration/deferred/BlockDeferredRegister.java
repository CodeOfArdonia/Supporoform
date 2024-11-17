package slimeknights.mantle.registration.deferred;

import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.*;
import net.minecraft.block.PressurePlateBlock.ActivationRule;
import net.minecraft.block.enums.Instrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SignItem;
import net.minecraft.item.TallBlockItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.StringIdentifiable;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.mantle.block.MantleStandingSignBlock;
import slimeknights.mantle.block.MantleWallSignBlock;
import slimeknights.mantle.block.StrippableLogBlock;
import slimeknights.mantle.block.entity.MantleSignBlockEntity;
import slimeknights.mantle.item.BurnableBlockItem;
import slimeknights.mantle.item.BurnableSignItem;
import slimeknights.mantle.item.BurnableTallBlockItem;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.registration.object.*;
import slimeknights.mantle.registration.object.WoodBlockObject.WoodVariant;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Deferred register to handle registering blocks with possible item forms
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class BlockDeferredRegister extends DeferredRegisterWrapper<Block> {

    protected final SynchronizedDeferredRegister<Item> itemRegister;

    public BlockDeferredRegister(String modID) {
        super(RegistryKeys.BLOCK, modID);
        this.itemRegister = SynchronizedDeferredRegister.create(RegistryKeys.ITEM, modID);
    }

    @Override
    public void register(IEventBus bus) {
        super.register(bus);
        this.itemRegister.register(bus);
    }


    /* Blocks with no items */

    /**
     * Registers a block with the block registry
     *
     * @param name  Block ID
     * @param block Block supplier
     * @param <B>   Block class
     * @return Block registry object
     */
    public <B extends Block> RegistryObject<B> registerNoItem(String name, Supplier<? extends B> block) {
        return this.register.register(name, block);
    }

    /**
     * Registers a block with the block registry
     *
     * @param name  Block ID
     * @param props Block properties
     * @return Block registry object
     */
    public RegistryObject<Block> registerNoItem(String name, Settings props) {
        return this.registerNoItem(name, () -> new Block(props));
    }


    /* Block item pairs */

    /**
     * Registers a block with the block registry, using the function for the BlockItem
     *
     * @param name  Block ID
     * @param block Block supplier
     * @param item  Function to create a BlockItem from a Block
     * @param <B>   Block class
     * @return Block item registry object pair
     */
    public <B extends Block> ItemObject<B> register(String name, Supplier<? extends B> block, final Function<? super B, ? extends BlockItem> item) {
        RegistryObject<B> blockObj = this.registerNoItem(name, block);
        this.itemRegister.register(name, () -> item.apply(blockObj.get()));
        return new ItemObject<>(blockObj);
    }

    /**
     * Registers a block with the block registry, using the function for the BlockItem
     *
     * @param name       Block ID
     * @param blockProps Block supplier
     * @param item       Function to create a BlockItem from a Block
     * @return Block item registry object pair
     */
    public ItemObject<Block> register(String name, Settings blockProps, Function<? super Block, ? extends BlockItem> item) {
        return this.register(name, () -> new Block(blockProps), item);
    }


    /* Building */

    /**
     * Registers a building block with slabs and stairs, using a custom block
     *
     * @param name  Block name
     * @param block Block supplier
     * @param item  Item block, used for all variants
     * @return Building block object
     */
    public BuildingBlockObject registerBuilding(String name, Supplier<? extends Block> block, Function<? super Block, ? extends BlockItem> item) {
        ItemObject<Block> blockObj = this.register(name, block, item);
        return new BuildingBlockObject(
                blockObj,
                this.register(name + "_slab", () -> new SlabBlock(Settings.copy(blockObj.get())), item),
                this.register(name + "_stairs", () -> new StairsBlock(() -> blockObj.get().defaultBlockState(), Settings.copy(blockObj.get())), item));
    }

    /**
     * Registers a block with slab, and stairs
     *
     * @param name  Name of the block
     * @param props Block properties
     * @param item  Function to get an item from the block
     * @return BuildingBlockObject class that returns different block types
     */
    public BuildingBlockObject registerBuilding(String name, Settings props, Function<? super Block, ? extends BlockItem> item) {
        ItemObject<Block> blockObj = this.register(name, props, item);
        return new BuildingBlockObject(blockObj,
                this.register(name + "_slab", () -> new SlabBlock(props), item),
                this.register(name + "_stairs", () -> new StairsBlock(() -> blockObj.get().defaultBlockState(), props), item)
        );
    }

    /**
     * Registers a building block with slabs, stairs and wall, using a custom block
     *
     * @param name  Block name
     * @param block Block supplier
     * @param item  Item block, used for all variants
     * @return Building block object
     */
    public WallBuildingBlockObject registerWallBuilding(String name, Supplier<? extends Block> block, Function<? super Block, ? extends BlockItem> item) {
        BuildingBlockObject obj = this.registerBuilding(name, block, item);
        return new WallBuildingBlockObject(obj, this.register(name + "_wall", () -> new WallBlock(Settings.copy(obj.get())), item));
    }

    /**
     * Registers a block with slab, stairs, and wall
     *
     * @param name  Name of the block
     * @param props Block properties
     * @param item  Function to get an item from the block
     * @return StoneBuildingBlockObject class that returns different block types
     */
    public WallBuildingBlockObject registerWallBuilding(String name, Settings props, Function<? super Block, ? extends BlockItem> item) {
        return new WallBuildingBlockObject(
                this.registerBuilding(name, props, item),
                this.register(name + "_wall", () -> new WallBlock(props), item)
        );
    }

    /**
     * Registers a building block with slabs, stairs and wall, using a custom block
     *
     * @param name  Block name
     * @param block Block supplier
     * @param item  Item block, used for all variants
     * @return Building block object
     */
    public FenceBuildingBlockObject registerFenceBuilding(String name, Supplier<? extends Block> block, Function<? super Block, ? extends BlockItem> item) {
        BuildingBlockObject obj = this.registerBuilding(name, block, item);
        return new FenceBuildingBlockObject(obj, this.register(name + "_fence", () -> new FenceBlock(Settings.copy(obj.get())), item));
    }

    /**
     * Registers a block with slab, stairs, and fence
     *
     * @param name  Name of the block
     * @param props Block properties
     * @param item  Function to get an item from the block
     * @return WoodBuildingBlockObject class that returns different block types
     */
    public FenceBuildingBlockObject registerFenceBuilding(String name, Settings props, Function<? super Block, ? extends BlockItem> item) {
        return new FenceBuildingBlockObject(
                this.registerBuilding(name, props, item),
                this.register(name + "_fence", () -> new FenceBlock(props), item)
        );
    }

    /**
     * Registers a new wood object
     *
     * @param name            Name of the wood object
     * @param behaviorCreator Logic to create the behavior
     * @param flammable       If true, this wood type is flammable
     * @return Wood object
     */
    public WoodBlockObject registerWood(String name, Function<WoodVariant, Settings> behaviorCreator, boolean flammable) {
        BlockSetType setType = new BlockSetType(this.resourceName(name));
        WoodType woodType = new WoodType(this.resourceName(name), setType);
        BlockSetType.register(setType);
        WoodType.register(woodType);
        RegistrationHelper.registerWoodType(woodType);
        Item.Settings itemProps = new Item.Settings();

        // many of these are already burnable via tags, but simplier to set them all here
        Function<Integer, Function<? super Block, ? extends BlockItem>> burnableItem;
        Function<? super Block, ? extends BlockItem> burnableTallItem;
        BiFunction<? super Block, ? super Block, ? extends BlockItem> burnableSignItem;
        Item.Settings signProps = new Item.Settings().maxCount(16);
        if (flammable) {
            burnableItem = burnTime -> block -> new BurnableBlockItem(block, itemProps, burnTime);
            burnableTallItem = block -> new BurnableTallBlockItem(block, itemProps, 200);
            burnableSignItem = (standing, wall) -> new BurnableSignItem(signProps, standing, wall, 200);
        } else {
            Function<? super Block, ? extends BlockItem> defaultItemBlock = block -> new BlockItem(block, itemProps);
            burnableItem = burnTime -> defaultItemBlock;
            burnableTallItem = block -> new TallBlockItem(block, itemProps);
            burnableSignItem = (standing, wall) -> new SignItem(signProps, standing, wall);
        }

        // planks
        Function<? super Block, ? extends BlockItem> burnable300 = burnableItem.apply(300);
        Settings planksProps = behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).strength(2.0f, 3.0f);
        BuildingBlockObject planks = this.registerBuilding(name + "_planks", planksProps, block -> burnableItem.apply(block instanceof SlabBlock ? 150 : 300).apply(block));
        ItemObject<FenceBlock> fence = this.register(name + "_fence", () -> new FenceBlock(Settings.copy(planks.get()).solid()), burnable300);
        // logs and wood
        Supplier<? extends PillarBlock> stripped = () -> new PillarBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).strength(2.0f));
        ItemObject<PillarBlock> strippedLog = this.register("stripped_" + name + "_log", stripped, burnable300);
        ItemObject<PillarBlock> strippedWood = this.register("stripped_" + name + "_wood", stripped, burnable300);
        ItemObject<PillarBlock> log = this.register(name + "_log", () -> new StrippableLogBlock(strippedLog, behaviorCreator.apply(WoodVariant.LOG).instrument(Instrument.BASS).strength(2.0f)), burnable300);
        ItemObject<PillarBlock> wood = this.register(name + "_wood", () -> new StrippableLogBlock(strippedWood, behaviorCreator.apply(WoodVariant.WOOD).instrument(Instrument.BASS).strength(2.0f)), burnable300);

        // doors
        ItemObject<DoorBlock> door = this.register(name + "_door", () -> new DoorBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).strength(3.0F).nonOpaque().pistonBehavior(PistonBehavior.DESTROY), setType), burnableTallItem);
        ItemObject<TrapdoorBlock> trapdoor = this.register(name + "_trapdoor", () -> new TrapdoorBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).strength(3.0F).nonOpaque().allowsSpawning(Blocks::never), setType), burnable300);
        ItemObject<FenceGateBlock> fenceGate = this.register(name + "_fence_gate", () -> new FenceGateBlock(Settings.copy(fence.get()), woodType), burnable300);
        // redstone
        Settings redstoneProps = behaviorCreator.apply(WoodVariant.PLANKS).solid().instrument(Instrument.BASS).noCollision().pistonBehavior(PistonBehavior.DESTROY).strength(0.5F);
        ItemObject<PressurePlateBlock> pressurePlate = this.register(name + "_pressure_plate", () -> new PressurePlateBlock(ActivationRule.EVERYTHING, redstoneProps, setType), burnable300);
        ItemObject<ButtonBlock> button = this.register(name + "_button", () -> new ButtonBlock(redstoneProps, setType, 30, true), burnableItem.apply(100));
        // signs
        RegistryObject<SignBlock> standingSign = this.registerNoItem(name + "_sign", () -> new MantleStandingSignBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).solid().noCollision().strength(1.0F), woodType));
        RegistryObject<WallSignBlock> wallSign = this.registerNoItem(name + "_wall_sign", () -> new MantleWallSignBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(Instrument.BASS).solid().noCollision().strength(1.0F).lootFrom(standingSign), woodType));
        // tell mantle to inject these into the TE
        MantleSignBlockEntity.registerSignBlock(standingSign);
        MantleSignBlockEntity.registerSignBlock(wallSign);
        // sign is included automatically in asItem of the standing sign
        this.itemRegister.register(name + "_sign", () -> burnableSignItem.apply(standingSign.get(), wallSign.get()));
        // finally, return
        return new WoodBlockObject(this.resource(name), woodType, planks, log, strippedLog, wood, strippedWood, fence, fenceGate, door, trapdoor, pressurePlate, button, standingSign, wallSign);
    }


    /* Enum */

    /**
     * Registers an item with multiple variants, prefixing the name with the value name
     *
     * @param values Enum values to use for this block
     * @param name   Name of the block
     * @param mapper Function to get a block for the given enum value
     * @param item   Function to get an item from the block
     * @return EnumObject mapping between different block types
     */
    public <T extends Enum<T> & StringIdentifiable, B extends Block> EnumObject<T, B> registerEnum(
            T[] values, String name, Function<T, ? extends B> mapper, Function<? super B, ? extends BlockItem> item) {
        return registerEnum(values, name, (fullName, value) -> this.register(fullName, () -> mapper.apply(value), item));
    }

    /**
     * Registers a block with multiple variants, suffixing the name with the value name
     *
     * @param name   Name of the block
     * @param values Enum values to use for this block
     * @param mapper Function to get a block for the given enum value
     * @param item   Function to get an item from the block
     * @return EnumObject mapping between different block types
     */
    public <T extends Enum<T> & StringIdentifiable, B extends Block> EnumObject<T, B> registerEnum(
            String name, T[] values, Function<T, ? extends B> mapper, Function<? super B, ? extends BlockItem> item) {
        return registerEnum(name, values, (fullName, value) -> this.register(fullName, () -> mapper.apply(value), item));
    }

    /**
     * Registers a block with enum variants, but no item form
     *
     * @param values Enum value list
     * @param name   Suffix after value name
     * @param mapper Function to map types to blocks
     * @param <T>    Type of enum
     * @param <B>    Type of block
     * @return Enum object
     */
    public <T extends Enum<T> & StringIdentifiable, B extends Block> EnumObject<T, B> registerEnumNoItem(T[] values, String name, Function<T, ? extends B> mapper) {
        return registerEnum(values, name, (fullName, value) -> this.registerNoItem(fullName, () -> mapper.apply(value)));
    }


    /* Metal */

    /**
     * Creates a new metal item object
     *
     * @param name          Metal name
     * @param tagName       Name to use for tags for this block
     * @param blockSupplier Supplier for the block
     * @param blockItem     Block item
     * @param itemProps     Properties for the item
     * @return Metal item object
     */
    public MetalItemObject registerMetal(String name, String tagName, Supplier<Block> blockSupplier, Function<Block, ? extends BlockItem> blockItem, Item.Settings itemProps) {
        ItemObject<Block> block = this.register(name + "_block", blockSupplier, blockItem);
        Supplier<Item> itemSupplier = () -> new Item(itemProps);
        RegistryObject<Item> ingot = this.itemRegister.register(name + "_ingot", itemSupplier);
        RegistryObject<Item> nugget = this.itemRegister.register(name + "_nugget", itemSupplier);
        return new MetalItemObject(tagName, block, ingot, nugget);
    }

    /**
     * Creates a new metal item object
     *
     * @param name          Metal name
     * @param blockSupplier Supplier for the block
     * @param blockItem     Block item
     * @param itemProps     Properties for the item
     * @return Metal item object
     */
    public MetalItemObject registerMetal(String name, Supplier<Block> blockSupplier, Function<Block, ? extends BlockItem> blockItem, Item.Settings itemProps) {
        return this.registerMetal(name, name, blockSupplier, blockItem, itemProps);
    }

    /**
     * Creates a new metal item object
     *
     * @param name       Metal name
     * @param tagName    Name to use for tags for this block
     * @param blockProps Properties for the block
     * @param blockItem  Block item
     * @param itemProps  Properties for the item
     * @return Metal item object
     */
    public MetalItemObject registerMetal(String name, String tagName, Settings blockProps, Function<Block, ? extends BlockItem> blockItem, Item.Settings itemProps) {
        return this.registerMetal(name, tagName, () -> new Block(blockProps), blockItem, itemProps);
    }

    /**
     * Creates a new metal item object
     *
     * @param name       Metal name
     * @param blockProps Properties for the block
     * @param blockItem  Block item
     * @param itemProps  Properties for the item
     * @return Metal item object
     */
    public MetalItemObject registerMetal(String name, Settings blockProps, Function<Block, ? extends BlockItem> blockItem, Item.Settings itemProps) {
        return this.registerMetal(name, name, blockProps, blockItem, itemProps);
    }
}
