package slimeknights.tconstruct.shared;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.block.Oxidizable.OxidationLevel;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BlockSoundGroup;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.mantle.item.EdibleItem;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.json.BlockOrEntityCondition;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.common.recipe.RecipeCacheInvalidator;
import slimeknights.tconstruct.library.json.condition.TagDifferencePresentCondition;
import slimeknights.tconstruct.library.json.condition.TagIntersectionPresentCondition;
import slimeknights.tconstruct.library.json.condition.TagNotEmptyCondition;
import slimeknights.tconstruct.library.json.loot.TagPreferenceLootEntry;
import slimeknights.tconstruct.library.json.predicate.TinkerPredicate;
import slimeknights.tconstruct.library.recipe.ingredient.BlockTagIngredient;
import slimeknights.tconstruct.library.recipe.ingredient.NoContainerIngredient;
import slimeknights.tconstruct.library.utils.SlimeBounceHandler;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.shared.block.BetterPaneBlock;
import slimeknights.tconstruct.shared.block.ClearGlassPaneBlock;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock;
import slimeknights.tconstruct.shared.block.ClearStainedGlassBlock.GlassColor;
import slimeknights.tconstruct.shared.block.ClearStainedGlassPaneBlock;
import slimeknights.tconstruct.shared.block.GlowBlock;
import slimeknights.tconstruct.shared.block.PlatformBlock;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.shared.block.SoulGlassBlock;
import slimeknights.tconstruct.shared.block.SoulGlassPaneBlock;
import slimeknights.tconstruct.shared.block.WaxedPlatformBlock;
import slimeknights.tconstruct.shared.block.WeatheringPlatformBlock;
import slimeknights.tconstruct.shared.command.TConstructCommand;
import slimeknights.tconstruct.shared.inventory.BlockContainerOpenedTrigger;
import slimeknights.tconstruct.shared.item.CheeseBlockItem;
import slimeknights.tconstruct.shared.item.CheeseItem;
import slimeknights.tconstruct.shared.item.TinkerBookItem;
import slimeknights.tconstruct.shared.item.TinkerBookItem.BookType;
import slimeknights.tconstruct.shared.particle.FluidParticleData;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * Contains items and blocks and stuff that is shared by multiple modules, but might be required individually
 */
@SuppressWarnings("unused")
public final class TinkerCommons extends TinkerModule {
    static final Logger log = Util.getLogger("tinker_commons");

    /*
     * Blocks
     */
    //public static final Material GLOW = (new Material.Builder(MapColor.CLEAR)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().build();
    public static final RegistryEntry<GlowBlock> glow = BLOCKS.registerNoItem("glow", () -> new GlowBlock(builder(MapColor.CLEAR, BlockSoundGroup.WOOL).strength(0.0F).luminance(s -> 14).nonOpaque()));
    // glass
    public static final ItemObject<GlassBlock> clearGlass = BLOCKS.register("clear_glass", () -> new GlassBlock(glassBuilder(MapColor.CLEAR)), GENERAL_BLOCK_ITEM);
    public static final ItemObject<TintedGlassBlock> clearTintedGlass = BLOCKS.register("clear_tinted_glass", () -> new TintedGlassBlock(glassBuilder(MapColor.GRAY).mapColor(MapColor.GRAY).nonOpaque().allowsSpawning(Blocks::never).solidBlock(Blocks::never).suffocates(Blocks::never).blockVision(Blocks::never)), GENERAL_BLOCK_ITEM);
    public static final ItemObject<ClearGlassPaneBlock> clearGlassPane = BLOCKS.register("clear_glass_pane", () -> new ClearGlassPaneBlock(glassBuilder(MapColor.CLEAR)), GENERAL_BLOCK_ITEM);
    public static final EnumObject<GlassColor, ClearStainedGlassBlock> clearStainedGlass = BLOCKS.registerEnum(GlassColor.values(), "clear_stained_glass", (color) -> new ClearStainedGlassBlock(glassBuilder(color.getDye().getMapColor()), color), GENERAL_BLOCK_ITEM);
    public static final EnumObject<GlassColor, ClearStainedGlassPaneBlock> clearStainedGlassPane = BLOCKS.registerEnum(GlassColor.values(), "clear_stained_glass_pane", (color) -> new ClearStainedGlassPaneBlock(glassBuilder(color.getDye().getMapColor()), color), GENERAL_BLOCK_ITEM);
    public static final ItemObject<GlassBlock> soulGlass = BLOCKS.register("soul_glass", () -> new SoulGlassBlock(glassBuilder(MapColor.BROWN).velocityMultiplier(0.2F).nonOpaque().blockVision((state, getter, pos) -> true)), GENERAL_TOOLTIP_BLOCK_ITEM);
    public static final ItemObject<ClearGlassPaneBlock> soulGlassPane = BLOCKS.register("soul_glass_pane", () -> new SoulGlassPaneBlock(glassBuilder(MapColor.BROWN).velocityMultiplier(0.2F)), GENERAL_TOOLTIP_BLOCK_ITEM);
    // panes
    public static final ItemObject<PaneBlock> goldBars = BLOCKS.register("gold_bars", () -> new PaneBlock(AbstractBlock.Settings.create().mapColor(MapColor.CLEAR).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.METAL).nonOpaque()), GENERAL_TOOLTIP_BLOCK_ITEM);
    public static final ItemObject<Block> obsidianPane = BLOCKS.register("obsidian_pane", () -> new BetterPaneBlock(builder(MapColor.SPRUCE_BROWN, BlockSoundGroup.STONE).requiresTool().nonOpaque().strength(25.0F, 400.0F)), GENERAL_BLOCK_ITEM);
    // platforms
    public static final ItemObject<PlatformBlock> goldPlatform = BLOCKS.register("gold_platform", () -> new PlatformBlock(AbstractBlock.Settings.create().mapColor(MapColor.GOLD).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_TOOLTIP_BLOCK_ITEM);
    public static final ItemObject<PlatformBlock> ironPlatform = BLOCKS.register("iron_platform", () -> new PlatformBlock(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).requiresTool().strength(5.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM);
    public static final ItemObject<PlatformBlock> cobaltPlatform = BLOCKS.register("cobalt_platform", () -> new PlatformBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLUE).requiresTool().strength(5.0f).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM);
    public static final EnumObject<OxidationLevel, PlatformBlock> copperPlatform = new EnumObject.Builder<OxidationLevel, PlatformBlock>(OxidationLevel.class)
            .put(OxidationLevel.UNAFFECTED, BLOCKS.register("copper_platform", () -> new WeatheringPlatformBlock(OxidationLevel.UNAFFECTED, AbstractBlock.Settings.create().mapColor(MapColor.ORANGE).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.EXPOSED, BLOCKS.register("exposed_copper_platform", () -> new WeatheringPlatformBlock(OxidationLevel.EXPOSED, AbstractBlock.Settings.create().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.WEATHERED, BLOCKS.register("weathered_copper_platform", () -> new WeatheringPlatformBlock(OxidationLevel.WEATHERED, AbstractBlock.Settings.create().mapColor(MapColor.DARK_AQUA).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.OXIDIZED, BLOCKS.register("oxidized_copper_platform", () -> new WeatheringPlatformBlock(OxidationLevel.OXIDIZED, AbstractBlock.Settings.create().mapColor(MapColor.TEAL).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM))
            .build();
    public static final EnumObject<OxidationLevel, PlatformBlock> waxedCopperPlatform = new EnumObject.Builder<OxidationLevel, PlatformBlock>(OxidationLevel.class)
            .put(OxidationLevel.UNAFFECTED, BLOCKS.register("waxed_copper_platform", () -> new WaxedPlatformBlock(OxidationLevel.UNAFFECTED, AbstractBlock.Settings.create().mapColor(MapColor.ORANGE).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.EXPOSED, BLOCKS.register("waxed_exposed_copper_platform", () -> new WaxedPlatformBlock(OxidationLevel.EXPOSED, AbstractBlock.Settings.create().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.WEATHERED, BLOCKS.register("waxed_weathered_copper_platform", () -> new WaxedPlatformBlock(OxidationLevel.WEATHERED, AbstractBlock.Settings.create().mapColor(MapColor.DARK_AQUA).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.OXIDIZED, BLOCKS.register("waxed_oxidized_copper_platform", () -> new WaxedPlatformBlock(OxidationLevel.OXIDIZED, AbstractBlock.Settings.create().mapColor(MapColor.TEAL).requiresTool().strength(3.0F, 6.0F).sounds(BlockSoundGroup.COPPER).nonOpaque()), GENERAL_BLOCK_ITEM))
            .build();


    /*
     * Items
     */
    // TODO TAB_GENERAL
    public static final ItemObject<EdibleItem> bacon = ITEMS.register("bacon", () -> new EdibleItem(TinkerFood.BACON));
    public static final ItemObject<EdibleItem> jeweledApple = ITEMS.register("jeweled_apple", () -> new EdibleItem(TinkerFood.JEWELED_APPLE));
    public static final ItemObject<Item> cheeseIngot = ITEMS.register("cheese_ingot", () -> new CheeseItem(new Settings().food(TinkerFood.CHEESE)));
    public static final ItemObject<Block> cheeseBlock = BLOCKS.register("cheese_block", () -> new TransparentBlock(AbstractBlock.Settings.create().mapColor(MapColor.YELLOW).strength(1.5F, 3.0F).velocityMultiplier(0.4F).jumpVelocityMultiplier(0.5F).sounds(BlockSoundGroup.HONEY).nonOpaque()), block -> new CheeseBlockItem(block, new Settings().food(TinkerFood.CHEESE)));

    private static final Item.Settings BOOK = new Item.Settings().maxCount(1);
    public static final ItemObject<TinkerBookItem> materialsAndYou = ITEMS.register("materials_and_you", () -> new TinkerBookItem(BOOK, BookType.MATERIALS_AND_YOU));
    public static final ItemObject<TinkerBookItem> punySmelting = ITEMS.register("puny_smelting", () -> new TinkerBookItem(BOOK, BookType.PUNY_SMELTING));
    public static final ItemObject<TinkerBookItem> mightySmelting = ITEMS.register("mighty_smelting", () -> new TinkerBookItem(BOOK, BookType.MIGHTY_SMELTING));
    public static final ItemObject<TinkerBookItem> tinkersGadgetry = ITEMS.register("tinkers_gadgetry", () -> new TinkerBookItem(BOOK, BookType.TINKERS_GADGETRY));
    public static final ItemObject<TinkerBookItem> fantasticFoundry = ITEMS.register("fantastic_foundry", () -> new TinkerBookItem(BOOK, BookType.FANTASTIC_FOUNDRY));
    public static final ItemObject<TinkerBookItem> encyclopedia = ITEMS.register("encyclopedia", () -> new TinkerBookItem(BOOK, BookType.ENCYCLOPEDIA));

    public static final RegistryEntry<ParticleType<FluidParticleData>> fluidParticle = PARTICLE_TYPES.register("fluid", FluidParticleData.Type::new);

    /* Loot conditions */
    public static final RegistryEntry<LootConditionType> lootConfig = LOOT_CONDITIONS.register(ConfigEnabledCondition.ID.getPath(), () -> new LootConditionType(ConfigEnabledCondition.SERIALIZER));
    public static final RegistryEntry<LootConditionType> lootBlockOrEntity = LOOT_CONDITIONS.register("block_or_entity", () -> new LootConditionType(new BlockOrEntityCondition.ConditionSerializer()));
    public static final RegistryEntry<LootConditionType> lootTagNotEmptyCondition = LOOT_CONDITIONS.register("tag_not_empty", () -> new LootConditionType(new TagNotEmptyCondition.ConditionSerializer()));
    public static final RegistryEntry<LootPoolEntryType> lootTagPreference = LOOT_ENTRIES.register("tag_preference", () -> new LootPoolEntryType(new TagPreferenceLootEntry.Serializer()));

    /* Slime Balls are edible, believe it or not */
    public static final EnumObject<SlimeType, Item> slimeball = new EnumObject.Builder<SlimeType, Item>(SlimeType.class)
            .put(SlimeType.EARTH, () -> Items.SLIME_BALL)
            .putAll(ITEMS.registerEnum(SlimeType.TINKER, "slime_ball", type -> new Item(GENERAL_PROPS)))
            .build();

    public static final BlockContainerOpenedTrigger CONTAINER_OPENED_TRIGGER = new BlockContainerOpenedTrigger();

    public TinkerCommons() {
        TConstructCommand.init();
//        MinecraftForge.EVENT_BUS.addListener(RecipeCacheInvalidator::onReloadListenerReload);
    }

//    @SubscribeEvent
//    void commonSetupEvent(FMLCommonSetupEvent event) {
//        SlimeBounceHandler.init();
//    }
//
//    @SubscribeEvent
//    void registerRecipeSerializers(RegisterEvent event) {
//        if (event.getRegistryKey() == Registry.RECIPE_SERIALIZER_REGISTRY) {
//            CraftingHelper.register(NoContainerIngredient.ID, NoContainerIngredient.Serializer.INSTANCE);
//            CraftingHelper.register(BlockTagIngredient.Serializer.ID, BlockTagIngredient.Serializer.INSTANCE);
//            CraftingHelper.register(ConfigEnabledCondition.SERIALIZER);
//            Criteria.register(CONTAINER_OPENED_TRIGGER);
//
//            CraftingHelper.register(TagIntersectionPresentCondition.SERIALIZER);
//            CraftingHelper.register(TagDifferencePresentCondition.SERIALIZER);
//            CraftingHelper.register(new TagNotEmptyCondition.ConditionSerializer());
//            // mantle
//            LivingEntityPredicate.LOADER.register(getResource("airborne"), TinkerPredicate.AIRBORNE.getLoader());
//        }
//    }

//    @SubscribeEvent
//    void gatherData(final GatherDataEvent event) {
//        DataGenerator generator = event.getGenerator();
//        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
//        boolean client = event.includeClient();
//        generator.addProvider(client, new ModelSpriteProvider(generator, existingFileHelper));
//        generator.addProvider(client, new TinkerItemModelProvider(generator, existingFileHelper));
//        generator.addProvider(client, new TinkerBlockStateProvider(generator, existingFileHelper));
//        generator.addProvider(event.includeServer(), new CommonRecipeProvider(generator));
//    }
}
