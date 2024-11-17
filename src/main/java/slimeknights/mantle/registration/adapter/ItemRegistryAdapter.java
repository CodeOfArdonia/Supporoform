package slimeknights.mantle.registration.adapter;

import net.minecraft.block.Block;
import net.minecraft.data.client.BlockStateVariantMap.TriFunction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.*;
import net.minecraft.item.Item.Settings;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.item.*;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.registration.object.*;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides utility registration methods when registering itemblocks.
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class ItemRegistryAdapter extends EnumRegistryAdapter<Item> {
    private final Settings defaultProps;

    /**
     * Registers a new item registry adapter with default mod ID and item properties
     *
     * @param registry Item registry instance
     */
    public ItemRegistryAdapter(Registry<Item> registry) {
        this(registry, null);
    }

    /**
     * Registers a new item registry adapter with default mod ID
     *
     * @param registry     Item registry instance
     * @param defaultProps Default item properties
     */
    public ItemRegistryAdapter(Registry<Item> registry, @Nullable Item.Settings defaultProps) {
        super(registry);
        this.defaultProps = Objects.requireNonNullElseGet(defaultProps, Settings::new);
    }

    /**
     * Registers a new item registry adapter with a specific mod ID
     *
     * @param registry     Item registry instance
     * @param modid        Mod ID override
     * @param defaultProps Default item properties
     */
    public ItemRegistryAdapter(Registry<Item> registry, String modid, @Nullable Item.Settings defaultProps) {
        super(registry, modid);
        this.defaultProps = Objects.requireNonNullElseGet(defaultProps, Settings::new);
    }

    /* Item helpers */

    /**
     * Registers a generic tooltip item using the default props
     *
     * @param name Item name
     * @return Registered item
     */
    public TooltipItem registerDefault(String name) {
        return this.register(this.defaultProps, name);
    }

    /**
     * Registers a generic tooltip item from the given props
     *
     * @param props Item properties
     * @param name  Item name
     * @return Registered item
     */
    public TooltipItem register(Settings props, String name) {
        return this.register(new TooltipItem(props), name);
    }

    /**
     * Registers an item with the default properties
     *
     * @param constructor Item constructor
     * @param name        Item name
     * @param <T>         Item type
     * @return Registered item
     */
    public <T extends Item> T registerDefault(Function<Settings, T> constructor, String name) {
        return this.register(constructor.apply(this.defaultProps), name);
    }


    /* Standard block items */

    /**
     * Registers a block item using the passed block as the name
     */
    protected <I extends BlockItem> I register(I entry, Block name) {
        return this.register(entry, Objects.requireNonNull(Registries.BLOCK.getId(name)));
    }

    /**
     * Registers a generic item block for a block.
     * If your block does not have its own item, just use this method to make it available as an item.
     * The item uses the same name as the block for registration.
     * The registered BlockItem has tooltip support by default, see {@link BlockTooltipItem}
     * It will be added to the creative itemgroup passed in in the constructor. If you want a different one, use the method with a ItemGroup parameter.
     *
     * @param block The block you want to have an item for
     * @return The registered item for the block
     */
    public BlockItem registerDefaultBlockItem(Block block) {
        return this.registerBlockItem(block, this.defaultProps);
    }

    /**
     * Registers a block item with default properties using the given constructor
     *
     * @param block       Block instance
     * @param constructor Constructor
     * @param <T>         Result block item type
     * @return Registered block item
     */
    public <T extends BlockItem> T registerBlockItem(Block block, BiFunction<Block, Settings, T> constructor) {
        return this.register(constructor.apply(block, this.defaultProps), block);
    }

    /**
     * Same as the variant without ItemGroup, but registers it for the given itemgroup.
     *
     * @param block The block you want to have an item for
     * @param props Item properties for the block
     */
    public BlockItem registerBlockItem(Block block, Settings props) {
        return this.register(new BlockTooltipItem(block, props), block);
    }

    /**
     * Shortcut method to register your own BlockItem, registering with the same name as the block it represents.
     *
     * @param blockItem Item block instance to register
     * @return Registered item block, should be the same as teh one passed in.
     */
    public <T extends BlockItem> T registerBlockItem(T blockItem) {
        return this.register(blockItem, blockItem.getBlock());
    }

    /* Block wrappers */

    /**
     * Registers block items for all entries in a building block object
     *
     * @param object Building block object instance
     */
    public void registerDefaultBlockItem(BuildingBlockObject object) {
        this.registerDefaultBlockItem(object.get());
        this.registerDefaultBlockItem(object.getSlab());
        this.registerDefaultBlockItem(object.getStairs());
    }

    /**
     * Registers block items for all entries in a wall building block object
     *
     * @param object Building block object instance
     */
    public void registerDefaultBlockItem(WallBuildingBlockObject object) {
        this.registerDefaultBlockItem((BuildingBlockObject) object);
        this.registerDefaultBlockItem(object.getWall());
    }

    /**
     * Registers block items for all entries in a fence building block object
     *
     * @param object Building block object instance
     */
    public void registerDefaultBlockItem(FenceBuildingBlockObject object) {
        this.registerDefaultBlockItem((BuildingBlockObject) object);
        this.registerDefaultBlockItem(object.getFence());
    }

    /**
     * Registers block items for all entries in a fence building block object
     *
     * @param object Building block object instance
     */
    public void registerDefaultBlockItem(WoodBlockObject object, boolean isBurnable) {
        // many of these are already burnable via tags, but simplier to set them all here
        BiFunction<? super Block, Integer, ? extends BlockItem> burnableItem;
        Function<? super Block, ? extends BlockItem> burnableTallItem;
        TriFunction<Settings, ? super Block, ? super Block, ? extends BlockItem> burnableSignItem;
        if (isBurnable) {
            burnableItem = (block, burnTime) -> new BurnableBlockItem(block, this.defaultProps, burnTime);
            burnableTallItem = (block) -> new BurnableTallBlockItem(block, this.defaultProps, 200);
            burnableSignItem = (props, standing, wall) -> new BurnableSignItem(props, standing, wall, 200);
        } else {
            burnableItem = (block, burnTime) -> new BlockItem(block, this.defaultProps);
            burnableTallItem = (block) -> new TallBlockItem(block, this.defaultProps);
            burnableSignItem = SignItem::new;
        }

        // planks
        BlockItem planks = this.registerBlockItem(burnableItem.apply(object.get(), 300));
        this.registerBlockItem(burnableItem.apply(object.getSlab(), 150));
        this.registerBlockItem(burnableItem.apply(object.getStairs(), 300));
        this.registerBlockItem(burnableItem.apply(object.getFence(), 300));
        // logs and wood
        this.registerBlockItem(burnableItem.apply(object.getLog(), 300));
        this.registerBlockItem(burnableItem.apply(object.getWood(), 300));
        this.registerBlockItem(burnableItem.apply(object.getStrippedLog(), 300));
        this.registerBlockItem(burnableItem.apply(object.getStrippedWood(), 300));
        // doors
        this.registerBlockItem(burnableTallItem.apply(object.getDoor()));
        this.registerBlockItem(burnableItem.apply(object.getTrapdoor(), 300));
        this.registerBlockItem(burnableItem.apply(object.getFenceGate(), 300));
        // redstone
        this.registerBlockItem(burnableItem.apply(object.getPressurePlate(), 300));
        this.registerBlockItem(burnableItem.apply(object.getButton(), 100));
        // sign
        this.registerBlockItem(burnableSignItem.apply(new Settings().maxCount(16), object.getSign(), object.getWallSign()));
    }

    /**
     * Registers block items for an enum object
     *
     * @param enumObject Enum object instance
     */
    public void registerDefaultBlockItem(EnumObject<?, ? extends Block> enumObject) {
        enumObject.values().forEach(this::registerDefaultBlockItem);
    }

    /**
     * Registers block items for an enum object
     *
     * @param enumObject Enum object instance
     * @param props      Item properties to use
     */
    public <B extends Block> void registerBlockItem(EnumObject<?, B> enumObject, Settings props) {
        enumObject.values().forEach(block -> this.registerBlockItem(block, props));
    }

    /**
     * Registers block items for an enum object
     *
     * @param enumObject Enum object instance
     * @param blockItem  Block item constructor
     */
    public <B extends Block> void registerBlockItem(EnumObject<?, B> enumObject, Function<B, ? extends BlockItem> blockItem) {
        enumObject.values().forEach(block -> this.registerBlockItem(blockItem.apply(block)));
    }


    /* Misc */

    /**
     * Registers the bucket for a fluid
     *
     * @param fluid    Fluid supplier
     * @param baseName Fluid name, unfortunately cannot be fetched from the fluid as it does not exist yet
     * @return Bucket instance
     */
    public BucketItem registerBucket(Supplier<? extends Fluid> fluid, String baseName) {
        return this.register(new BucketItem(fluid.get(), RegistrationHelper.BUCKET_PROPS), baseName + "_bucket");
    }

    /**
     * Registers a spawn egg for the entity type
     *
     * @param type      Entity type supplier
     * @param primary   Primary color
     * @param secondary Secondary color
     * @param baseName  Entity name, as it may or may not be present in the entity type
     * @return Spawn egg item instance
     */
    public SpawnEggItem registerSpawnEgg(Supplier<? extends EntityType<? extends MobEntity>> type, int primary, int secondary, String baseName) {
        return this.register(new SpawnEggItem(type.get(), primary, secondary, new Settings()), baseName + "_spawn_egg");
    }
}
