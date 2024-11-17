package slimeknights.tconstruct.gadgets;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.ItemGroup;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.util.SupplierCreativeTab;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.gadgets.block.FoodCakeBlock;
import slimeknights.tconstruct.gadgets.block.PunjiBlock;
import slimeknights.tconstruct.gadgets.capability.PiggybackCapability;
import slimeknights.tconstruct.gadgets.data.GadgetRecipeProvider;
import slimeknights.tconstruct.gadgets.entity.EFLNEntity;
import slimeknights.tconstruct.gadgets.entity.FancyItemFrameEntity;
import slimeknights.tconstruct.gadgets.entity.FrameType;
import slimeknights.tconstruct.gadgets.entity.GlowballEntity;
import slimeknights.tconstruct.gadgets.entity.shuriken.FlintShurikenEntity;
import slimeknights.tconstruct.gadgets.entity.shuriken.QuartzShurikenEntity;
import slimeknights.tconstruct.gadgets.item.EFLNItem;
import slimeknights.tconstruct.gadgets.item.FancyItemFrameItem;
import slimeknights.tconstruct.gadgets.item.GlowBallItem;
import slimeknights.tconstruct.gadgets.item.PiggyBackPackItem;
import slimeknights.tconstruct.gadgets.item.PiggyBackPackItem.CarryPotionEffect;
import slimeknights.tconstruct.gadgets.item.ShurikenItem;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.shared.TinkerFood;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.function.Function;

/**
 * Contains any special tools unrelated to the base tools
 */
@SuppressWarnings("unused")
public final class TinkerGadgets extends TinkerModule {
    /**
     * Tab for all special tools added by the mod
     */
    public static final ItemGroup TAB_GADGETS = new SupplierCreativeTab(TConstruct.MOD_ID, "gadgets", () -> new ItemStack(TinkerGadgets.itemFrame.get(FrameType.CLEAR)));
    static final Logger log = Util.getLogger("tinker_gadgets");

    /*
     * Block base properties
     */
    private static final Item.Settings GADGET_PROPS = new Item.Settings().tab(TAB_GADGETS);
    private static final Item.Settings UNSTACKABLE_PROPS = new Item.Settings().tab(TAB_GADGETS).stacksTo(1);
    private static final Function<Block, ? extends BlockItem> DEFAULT_BLOCK_ITEM = (b) -> new BlockItem(b, GADGET_PROPS);
    private static final Function<Block, ? extends BlockItem> TOOLTIP_BLOCK_ITEM = (b) -> new BlockTooltipItem(b, GADGET_PROPS);
    private static final Function<Block, ? extends BlockItem> UNSTACKABLE_BLOCK_ITEM = (b) -> new BlockTooltipItem(b, UNSTACKABLE_PROPS);

    /*
     * Blocks
     */
    public static final ItemObject<PunjiBlock> punji = BLOCKS.register("punji", () -> new PunjiBlock(builder(Material.PLANT, BlockSoundGroup.GRASS).strength(3.0F).speedFactor(0.4F).noOcclusion()), TOOLTIP_BLOCK_ITEM);

    /*
     * Items
     */
    public static final ItemObject<PiggyBackPackItem> piggyBackpack = ITEMS.register("piggy_backpack", () -> new PiggyBackPackItem(new Settings().tab(TinkerGadgets.TAB_GADGETS).stacksTo(16)));
    public static final EnumObject<FrameType, FancyItemFrameItem> itemFrame = ITEMS.registerEnum(FrameType.values(), "item_frame", (type) -> new FancyItemFrameItem(GADGET_PROPS, (world, pos, dir) -> new FancyItemFrameEntity(world, pos, dir, type)));

    // throwballs
    public static final ItemObject<GlowBallItem> glowBall = ITEMS.register("glow_ball", GlowBallItem::new);
    public static final ItemObject<EFLNItem> efln = ITEMS.register("efln_ball", EFLNItem::new);

    // foods
    private static final AbstractBlock.Settings CAKE = builder(Material.CAKE, BlockSoundGroup.WOOL).strength(0.5F);
    public static final EnumObject<FoliageType, FoodCakeBlock> cake = BLOCKS.registerEnum(FoliageType.values(), "cake", type -> new FoodCakeBlock(CAKE, TinkerFood.getCake(type)), UNSTACKABLE_BLOCK_ITEM);
    public static final ItemObject<FoodCakeBlock> magmaCake = BLOCKS.register("magma_cake", () -> new FoodCakeBlock(CAKE, TinkerFood.MAGMA_CAKE), UNSTACKABLE_BLOCK_ITEM);

    // Shurikens
    private static final Item.Settings THROWABLE_PROPS = new Item.Settings().maxCount(16).tab(TAB_GADGETS);
    public static final ItemObject<ShurikenItem> quartzShuriken = ITEMS.register("quartz_shuriken", () -> new ShurikenItem(THROWABLE_PROPS, QuartzShurikenEntity::new));
    public static final ItemObject<ShurikenItem> flintShuriken = ITEMS.register("flint_shuriken", () -> new ShurikenItem(THROWABLE_PROPS, FlintShurikenEntity::new));

    /*
     * Entities
     */
    public static final RegistryObject<EntityType<FancyItemFrameEntity>> itemFrameEntity = ENTITIES.register("fancy_item_frame", () ->
            EntityType.Builder.<FancyItemFrameEntity>create(
                            FancyItemFrameEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.5F)
                    .setTrackingRange(10)
                    .setUpdateInterval(Integer.MAX_VALUE)
                    .setCustomClientFactory((spawnEntity, world) -> new FancyItemFrameEntity(TinkerGadgets.itemFrameEntity.get(), world))
                    .setShouldReceiveVelocityUpdates(false)
    );
    public static final RegistryObject<EntityType<GlowballEntity>> glowBallEntity = ENTITIES.register("glow_ball", () ->
            EntityType.Builder.<GlowballEntity>create(GlowballEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .setTrackingRange(4)
                    .setUpdateInterval(10)
                    .setCustomClientFactory((spawnEntity, world) -> new GlowballEntity(TinkerGadgets.glowBallEntity.get(), world))
                    .setShouldReceiveVelocityUpdates(true)
    );
    public static final RegistryObject<EntityType<EFLNEntity>> eflnEntity = ENTITIES.register("efln_ball", () ->
            EntityType.Builder.<EFLNEntity>create(EFLNEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .setTrackingRange(4)
                    .setUpdateInterval(10)
                    .setCustomClientFactory((spawnEntity, world) -> new EFLNEntity(TinkerGadgets.eflnEntity.get(), world))
                    .setShouldReceiveVelocityUpdates(true)
    );
    public static final RegistryObject<EntityType<QuartzShurikenEntity>> quartzShurikenEntity = ENTITIES.register("quartz_shuriken", () ->
            EntityType.Builder.<QuartzShurikenEntity>create(QuartzShurikenEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .setTrackingRange(4)
                    .setUpdateInterval(10)
                    .setCustomClientFactory((spawnEntity, world) -> new QuartzShurikenEntity(TinkerGadgets.quartzShurikenEntity.get(), world))
                    .setShouldReceiveVelocityUpdates(true)
    );
    public static final RegistryObject<EntityType<FlintShurikenEntity>> flintShurikenEntity = ENTITIES.register("flint_shuriken", () ->
            EntityType.Builder.<FlintShurikenEntity>create(FlintShurikenEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .setTrackingRange(4)
                    .setUpdateInterval(10)
                    .setCustomClientFactory((spawnEntity, world) -> new FlintShurikenEntity(TinkerGadgets.flintShurikenEntity.get(), world))
                    .setShouldReceiveVelocityUpdates(true)
    );

    /*
     * Potions
     */
    public static final RegistryObject<CarryPotionEffect> carryEffect = MOB_EFFECTS.register("carry", CarryPotionEffect::new);

    /*
     * Events
     */
    @SubscribeEvent
    void commonSetup(final FMLCommonSetupEvent event) {
        PiggybackCapability.register();
        event.enqueueWork(() -> {
            cake.forEach(block -> ComposterBlock.add(1.0f, block));
            ComposterBlock.add(1.0f, magmaCake.get());
        });
    }

    @SubscribeEvent
    void gatherData(final GatherDataEvent event) {
        DataGenerator datagenerator = event.getGenerator();
        datagenerator.addProvider(event.includeServer(), new GadgetRecipeProvider(datagenerator));
    }
}