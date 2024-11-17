package slimeknights.tconstruct.common;

import com.mojang.serialization.Codec;
import io.github.fabricators_of_create.porting_lib.loot.IGlobalLootModifier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.particle.ParticleType;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import slimeknights.mantle.item.BlockTooltipItem;
import slimeknights.mantle.item.TooltipItem;
import slimeknights.mantle.registration.deferred.*;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.registration.*;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.block.SlimeType;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Contains base helpers for all Tinker modules. Should not be extended by other mods, this is only for internal usage.
 */
public abstract class TinkerModule {
    protected TinkerModule() {
        TConstruct.sealTinkersClass(this, "TinkerModule", "This is a bug with the mod containing that class, they should create their own deferred registers.");
    }

    // deferred register instances
    // gameplay singleton
    protected static final BlockDeferredRegisterExtension BLOCKS = new BlockDeferredRegisterExtension(TConstruct.MOD_ID);
    protected static final ItemDeferredRegisterExtension ITEMS = new ItemDeferredRegisterExtension(TConstruct.MOD_ID);
    protected static final FluidDeferredRegister FLUIDS = new FluidDeferredRegister(TConstruct.MOD_ID);
    protected static final EnumDeferredRegister<StatusEffect> MOB_EFFECTS = new EnumDeferredRegister<>(RegistryKeys.STATUS_EFFECT, TConstruct.MOD_ID);
    protected static final SynchronizedDeferredRegister<ParticleType<?>> PARTICLE_TYPES = SynchronizedDeferredRegister.create(Registries.PARTICLE_TYPE, TConstruct.MOD_ID);
    protected static final SynchronizedDeferredRegister<TrackedDataHandler<?>> DATA_SERIALIZERS = SynchronizedDeferredRegister.create(Keys.ENTITY_DATA_SERIALIZERS, TConstruct.MOD_ID);
    // gameplay instances
    protected static final BlockEntityTypeDeferredRegister BLOCK_ENTITIES = new BlockEntityTypeDeferredRegister(TConstruct.MOD_ID);
    protected static final EntityTypeDeferredRegister ENTITIES = new EntityTypeDeferredRegister(TConstruct.MOD_ID);
    protected static final MenuTypeDeferredRegister MENUS = new MenuTypeDeferredRegister(TConstruct.MOD_ID);
    // datapacks
    protected static final SynchronizedDeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = SynchronizedDeferredRegister.create(Registries.RECIPE_SERIALIZER, TConstruct.MOD_ID);
    protected static final SynchronizedDeferredRegister<Codec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIERS = SynchronizedDeferredRegister.create(Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TConstruct.MOD_ID);
    protected static final SynchronizedDeferredRegister<LootConditionType> LOOT_CONDITIONS = SynchronizedDeferredRegister.create(Registries.LOOT_CONDITION_TYPE, TConstruct.MOD_ID);
    protected static final SynchronizedDeferredRegister<LootFunctionType> LOOT_FUNCTIONS = SynchronizedDeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, TConstruct.MOD_ID);
    protected static final SynchronizedDeferredRegister<LootPoolEntryType> LOOT_ENTRIES = SynchronizedDeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, TConstruct.MOD_ID);
    // worldgen
    protected static final PlacedFeatureDeferredRegister PLACED_FEATURES = new PlacedFeatureDeferredRegister(TConstruct.MOD_ID);
    protected static final ConfiguredFeatureDeferredRegister CONFIGURED_FEATURES = new ConfiguredFeatureDeferredRegister(TConstruct.MOD_ID);
    //

    /**
     * Creative tab for items that do not fit in another tab
     */
    @SuppressWarnings("WeakerAccess")
    public static final ItemGroup TAB_GENERAL = new SupplierCreativeTab(TConstruct.MOD_ID, "general", () -> new ItemStack(TinkerCommons.slimeball.get(SlimeType.SKY)));

    // base item properties
    protected static final Item.Settings HIDDEN_PROPS = new Item.Settings();
    protected static final Item.Settings GENERAL_PROPS = new Item.Settings();
    // TODO
//    protected static final Item.Settings GENERAL_PROPS = new Item.Settings().tab(TAB_GENERAL);
    protected static final Function<Block, ? extends BlockItem> HIDDEN_BLOCK_ITEM = (b) -> new BlockItem(b, HIDDEN_PROPS);
    protected static final Function<Block, ? extends BlockItem> GENERAL_BLOCK_ITEM = (b) -> new BlockItem(b, GENERAL_PROPS);
    protected static final Function<Block, ? extends BlockItem> GENERAL_TOOLTIP_BLOCK_ITEM = (b) -> new BlockTooltipItem(b, GENERAL_PROPS);
    protected static final Supplier<Item> TOOLTIP_ITEM = () -> new TooltipItem(GENERAL_PROPS);

    /**
     * Called during construction to initialize the registers for this mod
     */
    public static void initRegisters() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        // gameplay singleton
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FLUIDS.register(bus);
        MOB_EFFECTS.register(bus);
        PARTICLE_TYPES.register(bus);
        DATA_SERIALIZERS.register(bus);
        // gameplay instance
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        // datapacks
        RECIPE_SERIALIZERS.register(bus);
        GLOBAL_LOOT_MODIFIERS.register(bus);
        LOOT_CONDITIONS.register(bus);
        LOOT_FUNCTIONS.register(bus);
        LOOT_ENTRIES.register(bus);
        TinkerRecipeTypes.init(bus);
        // worldgen
        CONFIGURED_FEATURES.register(bus);
        PLACED_FEATURES.register(bus);
    }

    /**
     * We use this builder to ensure that our blocks all have the most important properties set.
     * This way it'll stick out if a block doesn't have a tooltype or sound set.
     * It may be a bit less clear at first, since the actual builder methods tell you what each value means,
     * but as long as we don't statically import the enums it should be just as readable.
     */
    protected static AbstractBlock.Settings builder(BlockSoundGroup soundType) {
        return AbstractBlock.Settings.create().sounds(soundType);
    }

    /**
     * Same as above, but with a color
     */
    protected static AbstractBlock.Settings builder(MapColor color, BlockSoundGroup soundType) {
        return Block.Settings.create().mapColor(color).sounds(soundType);
    }

    /**
     * Builder that pre-supplies metal properties
     */
    protected static AbstractBlock.Settings metalBuilder(MapColor color) {
        return builder(color, BlockSoundGroup.METAL).requiresTool().strength(5.0f);
    }

    /**
     * Builder that pre-supplies glass properties
     */
    protected static AbstractBlock.Settings glassBuilder(MapColor color) {
        return builder(color, BlockSoundGroup.GLASS)
                .strength(0.3F).nonOpaque().allowsSpawning(Blocks::never)
                .solidBlock(Blocks::never).suffocates(Blocks::never).blockVision(Blocks::never);
    }

    /**
     * Builder that pre-supplies glass properties
     */
    protected static AbstractBlock.Settings woodBuilder(MapColor color) {
        return builder(color, BlockSoundGroup.WOOD).requiresTool().strength(2.0F, 7.0F);
    }

    /**
     * Creates a Tinkers Construct resource location
     *
     * @param path Resource path
     * @return Tinkers Construct resource location
     */
    protected static Identifier resource(String path) {
        return TConstruct.getResource(path);
    }
}
