package slimeknights.tconstruct.shared;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.Oxidizable.OxidationLevel;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.TintedGlassBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.particle.ParticleType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.mantle.item.EdibleItem;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.data.model.ModelSpriteProvider;
import slimeknights.tconstruct.common.data.model.TinkerBlockStateProvider;
import slimeknights.tconstruct.common.data.model.TinkerItemModelProvider;
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
import slimeknights.tconstruct.shared.data.CommonRecipeProvider;
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
    public static final Material GLOW = (new Material.Builder(MaterialColor.NONE)).noCollider().notSolidBlocking().nonSolid().destroyOnPush().replaceable().build();
    public static final RegistryObject<GlowBlock> glow = BLOCKS.registerNoItem("glow", () -> new GlowBlock(builder(GLOW, BlockSoundGroup.WOOL).strength(0.0F).lightLevel(s -> 14).noOcclusion()));
    // glass
    public static final ItemObject<GlassBlock> clearGlass = BLOCKS.register("clear_glass", () -> new GlassBlock(glassBuilder(MaterialColor.NONE)), GENERAL_BLOCK_ITEM);
    public static final ItemObject<TintedGlassBlock> clearTintedGlass = BLOCKS.register("clear_tinted_glass", () -> new TintedGlassBlock(glassBuilder(MaterialColor.COLOR_GRAY).color(MaterialColor.COLOR_GRAY).noOcclusion().isValidSpawn(Blocks::never).isRedstoneConductor(Blocks::never).isSuffocating(Blocks::never).isViewBlocking(Blocks::never)), GENERAL_BLOCK_ITEM);
    public static final ItemObject<ClearGlassPaneBlock> clearGlassPane = BLOCKS.register("clear_glass_pane", () -> new ClearGlassPaneBlock(glassBuilder(MaterialColor.NONE)), GENERAL_BLOCK_ITEM);
    public static final EnumObject<GlassColor, ClearStainedGlassBlock> clearStainedGlass = BLOCKS.registerEnum(GlassColor.values(), "clear_stained_glass", (color) -> new ClearStainedGlassBlock(glassBuilder(color.getDye().getMaterialColor()), color), GENERAL_BLOCK_ITEM);
    public static final EnumObject<GlassColor, ClearStainedGlassPaneBlock> clearStainedGlassPane = BLOCKS.registerEnum(GlassColor.values(), "clear_stained_glass_pane", (color) -> new ClearStainedGlassPaneBlock(glassBuilder(color.getDye().getMaterialColor()), color), GENERAL_BLOCK_ITEM);
    public static final ItemObject<GlassBlock> soulGlass = BLOCKS.register("soul_glass", () -> new SoulGlassBlock(glassBuilder(MaterialColor.COLOR_BROWN).speedFactor(0.2F).noCollission().isViewBlocking((state, getter, pos) -> true)), GENERAL_TOOLTIP_BLOCK_ITEM);
    public static final ItemObject<ClearGlassPaneBlock> soulGlassPane = BLOCKS.register("soul_glass_pane", () -> new SoulGlassPaneBlock(glassBuilder(MaterialColor.COLOR_BROWN).speedFactor(0.2F)), GENERAL_TOOLTIP_BLOCK_ITEM);
    // panes
    public static final ItemObject<PaneBlock> goldBars = BLOCKS.register("gold_bars", () -> new PaneBlock(AbstractBlock.Settings.create(Material.METAL, MaterialColor.NONE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.METAL).noOcclusion()), GENERAL_TOOLTIP_BLOCK_ITEM);
    public static final ItemObject<Block> obsidianPane = BLOCKS.register("obsidian_pane", () -> new BetterPaneBlock(builder(Material.STONE, MaterialColor.PODZOL, BlockSoundGroup.STONE).requiresCorrectToolForDrops().noOcclusion().strength(25.0F, 400.0F)), GENERAL_BLOCK_ITEM);
    // platforms
    public static final ItemObject<PlatformBlock> goldPlatform = BLOCKS.register("gold_platform", () -> new PlatformBlock(AbstractBlock.Settings.create(Material.METAL, MaterialColor.GOLD).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_TOOLTIP_BLOCK_ITEM);
    public static final ItemObject<PlatformBlock> ironPlatform = BLOCKS.register("iron_platform", () -> new PlatformBlock(AbstractBlock.Settings.create(Material.METAL, MaterialColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM);
    public static final ItemObject<PlatformBlock> cobaltPlatform = BLOCKS.register("cobalt_platform", () -> new PlatformBlock(AbstractBlock.Settings.create(Material.METAL, MaterialColor.COLOR_BLUE).requiresCorrectToolForDrops().strength(5.0f).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM);
    public static final EnumObject<OxidationLevel, PlatformBlock> copperPlatform = new EnumObject.Builder<OxidationLevel, PlatformBlock>(OxidationLevel.class)
            .put(OxidationLevel.UNAFFECTED, BLOCKS.register("copper_platform", () -> new WeatheringPlatformBlock(OxidationLevel.UNAFFECTED, AbstractBlock.Settings.create(Material.METAL, MaterialColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.EXPOSED, BLOCKS.register("exposed_copper_platform", () -> new WeatheringPlatformBlock(OxidationLevel.EXPOSED, AbstractBlock.Settings.create(Material.METAL, MaterialColor.TERRACOTTA_LIGHT_GRAY).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.WEATHERED, BLOCKS.register("weathered_copper_platform", () -> new WeatheringPlatformBlock(OxidationLevel.WEATHERED, AbstractBlock.Settings.create(Material.METAL, MaterialColor.WARPED_STEM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.OXIDIZED, BLOCKS.register("oxidized_copper_platform", () -> new WeatheringPlatformBlock(OxidationLevel.OXIDIZED, AbstractBlock.Settings.create(Material.METAL, MaterialColor.WARPED_NYLIUM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
            .build();
    public static final EnumObject<OxidationLevel, PlatformBlock> waxedCopperPlatform = new EnumObject.Builder<OxidationLevel, PlatformBlock>(OxidationLevel.class)
            .put(OxidationLevel.UNAFFECTED, BLOCKS.register("waxed_copper_platform", () -> new WaxedPlatformBlock(OxidationLevel.UNAFFECTED, AbstractBlock.Settings.create(Material.METAL, MaterialColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.EXPOSED, BLOCKS.register("waxed_exposed_copper_platform", () -> new WaxedPlatformBlock(OxidationLevel.EXPOSED, AbstractBlock.Settings.create(Material.METAL, MaterialColor.TERRACOTTA_LIGHT_GRAY).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.WEATHERED, BLOCKS.register("waxed_weathered_copper_platform", () -> new WaxedPlatformBlock(OxidationLevel.WEATHERED, AbstractBlock.Settings.create(Material.METAL, MaterialColor.WARPED_STEM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
            .put(OxidationLevel.OXIDIZED, BLOCKS.register("waxed_oxidized_copper_platform", () -> new WaxedPlatformBlock(OxidationLevel.OXIDIZED, AbstractBlock.Settings.create(Material.METAL, MaterialColor.WARPED_NYLIUM).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(BlockSoundGroup.COPPER).noOcclusion()), GENERAL_BLOCK_ITEM))
            .build();


    /*
     * Items
     */
    public static final ItemObject<EdibleItem> bacon = ITEMS.register("bacon", () -> new EdibleItem(TinkerFood.BACON, TAB_GENERAL));
    public static final ItemObject<EdibleItem> jeweledApple = ITEMS.register("jeweled_apple", () -> new EdibleItem(TinkerFood.JEWELED_APPLE, TAB_GENERAL));
    public static final ItemObject<Item> cheeseIngot = ITEMS.register("cheese_ingot", () -> new CheeseItem(new Settings().tab(TAB_GENERAL).food(TinkerFood.CHEESE)));
    public static final ItemObject<Block> cheeseBlock = BLOCKS.register("cheese_block", () -> new TransparentBlock(AbstractBlock.Settings.create(Material.DIRT, MaterialColor.COLOR_YELLOW).strength(1.5F, 3.0F).speedFactor(0.4F).jumpFactor(0.5F).sound(BlockSoundGroup.HONEY).noOcclusion()), block -> new CheeseBlockItem(block, new Settings().tab(TAB_GENERAL).food(TinkerFood.CHEESE)));

    private static final Item.Settings BOOK = new Item.Settings().tab(TAB_GENERAL).stacksTo(1);
    public static final ItemObject<TinkerBookItem> materialsAndYou = ITEMS.register("materials_and_you", () -> new TinkerBookItem(BOOK, BookType.MATERIALS_AND_YOU));
    public static final ItemObject<TinkerBookItem> punySmelting = ITEMS.register("puny_smelting", () -> new TinkerBookItem(BOOK, BookType.PUNY_SMELTING));
    public static final ItemObject<TinkerBookItem> mightySmelting = ITEMS.register("mighty_smelting", () -> new TinkerBookItem(BOOK, BookType.MIGHTY_SMELTING));
    public static final ItemObject<TinkerBookItem> tinkersGadgetry = ITEMS.register("tinkers_gadgetry", () -> new TinkerBookItem(BOOK, BookType.TINKERS_GADGETRY));
    public static final ItemObject<TinkerBookItem> fantasticFoundry = ITEMS.register("fantastic_foundry", () -> new TinkerBookItem(BOOK, BookType.FANTASTIC_FOUNDRY));
    public static final ItemObject<TinkerBookItem> encyclopedia = ITEMS.register("encyclopedia", () -> new TinkerBookItem(BOOK, BookType.ENCYCLOPEDIA));

    public static final RegistryObject<ParticleType<FluidParticleData>> fluidParticle = PARTICLE_TYPES.register("fluid", FluidParticleData.Type::new);

    /* Loot conditions */
    public static final RegistryObject<LootConditionType> lootConfig = LOOT_CONDITIONS.register(ConfigEnabledCondition.ID.getPath(), () -> new LootConditionType(ConfigEnabledCondition.SERIALIZER));
    public static final RegistryObject<LootConditionType> lootBlockOrEntity = LOOT_CONDITIONS.register("block_or_entity", () -> new LootConditionType(new BlockOrEntityCondition.ConditionSerializer()));
    public static final RegistryObject<LootConditionType> lootTagNotEmptyCondition = LOOT_CONDITIONS.register("tag_not_empty", () -> new LootConditionType(new TagNotEmptyCondition.ConditionSerializer()));
    public static final RegistryObject<LootPoolEntryType> lootTagPreference = LOOT_ENTRIES.register("tag_preference", () -> new LootPoolEntryType(new TagPreferenceLootEntry.Serializer()));

    /* Slime Balls are edible, believe it or not */
    public static final EnumObject<SlimeType, Item> slimeball = new EnumObject.Builder<SlimeType, Item>(SlimeType.class)
            .put(SlimeType.EARTH, () -> Items.SLIME_BALL)
            .putAll(ITEMS.registerEnum(SlimeType.TINKER, "slime_ball", type -> new Item(GENERAL_PROPS)))
            .build();

    public static final BlockContainerOpenedTrigger CONTAINER_OPENED_TRIGGER = new BlockContainerOpenedTrigger();

    public TinkerCommons() {
        TConstructCommand.init();
        MinecraftForge.EVENT_BUS.addListener(RecipeCacheInvalidator::onReloadListenerReload);
    }

    @SubscribeEvent
    void commonSetupEvent(FMLCommonSetupEvent event) {
        SlimeBounceHandler.init();
    }

    @SubscribeEvent
    void registerRecipeSerializers(RegisterEvent event) {
        if (event.getRegistryKey() == Registry.RECIPE_SERIALIZER_REGISTRY) {
            CraftingHelper.register(NoContainerIngredient.ID, NoContainerIngredient.Serializer.INSTANCE);
            CraftingHelper.register(BlockTagIngredient.Serializer.ID, BlockTagIngredient.Serializer.INSTANCE);
            CraftingHelper.register(ConfigEnabledCondition.SERIALIZER);
            Criteria.register(CONTAINER_OPENED_TRIGGER);

            CraftingHelper.register(TagIntersectionPresentCondition.SERIALIZER);
            CraftingHelper.register(TagDifferencePresentCondition.SERIALIZER);
            CraftingHelper.register(new TagNotEmptyCondition.ConditionSerializer());
            // mantle
            LivingEntityPredicate.LOADER.register(getResource("airborne"), TinkerPredicate.AIRBORNE.getLoader());
        }
    }

    @SubscribeEvent
    void gatherData(final GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        boolean client = event.includeClient();
        generator.addProvider(client, new ModelSpriteProvider(generator, existingFileHelper));
        generator.addProvider(client, new TinkerItemModelProvider(generator, existingFileHelper));
        generator.addProvider(client, new TinkerBlockStateProvider(generator, existingFileHelper));
        generator.addProvider(event.includeServer(), new CommonRecipeProvider(generator));
    }
}
