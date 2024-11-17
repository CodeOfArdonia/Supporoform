package slimeknights.tconstruct.shared;

import net.minecraft.block.MapColor;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.registration.object.MetalItemObject;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient;
import slimeknights.tconstruct.shared.block.OrientableBlock;
import slimeknights.tconstruct.shared.block.SlimesteelBlock;

/**
 * Contains bommon blocks and items used in crafting materials
 */
@SuppressWarnings("unused")
public final class TinkerMaterials extends TinkerModule {
    // ores
    public static final MetalItemObject cobalt = BLOCKS.registerMetal("cobalt", metalBuilder(MapColor.BLUE), GENERAL_TOOLTIP_BLOCK_ITEM, GENERAL_PROPS);
    // tier 3
    public static final MetalItemObject slimesteel = BLOCKS.registerMetal("slimesteel", () -> new SlimesteelBlock(metalBuilder(MapColor.BRIGHT_TEAL).nonOpaque()), GENERAL_TOOLTIP_BLOCK_ITEM, GENERAL_PROPS);
    public static final MetalItemObject amethystBronze = BLOCKS.registerMetal("amethyst_bronze", metalBuilder(MapColor.PURPLE), GENERAL_TOOLTIP_BLOCK_ITEM, GENERAL_PROPS);
    public static final MetalItemObject roseGold = BLOCKS.registerMetal("rose_gold", metalBuilder(MapColor.TERRACOTTA_WHITE), GENERAL_TOOLTIP_BLOCK_ITEM, GENERAL_PROPS);
    public static final MetalItemObject pigIron = BLOCKS.registerMetal("pig_iron", () -> new OrientableBlock(metalBuilder(MapColor.PINK)), GENERAL_TOOLTIP_BLOCK_ITEM, GENERAL_PROPS);
    // tier 4
    public static final MetalItemObject queensSlime = BLOCKS.registerMetal("queens_slime", metalBuilder(MapColor.GREEN), GENERAL_TOOLTIP_BLOCK_ITEM, GENERAL_PROPS);
    public static final MetalItemObject manyullyn = BLOCKS.registerMetal("manyullyn", metalBuilder(MapColor.PURPLE), GENERAL_TOOLTIP_BLOCK_ITEM, GENERAL_PROPS);
    public static final MetalItemObject hepatizon = BLOCKS.registerMetal("hepatizon", metalBuilder(MapColor.TERRACOTTA_BLUE), GENERAL_TOOLTIP_BLOCK_ITEM, GENERAL_PROPS);
    public static final MetalItemObject soulsteel = BLOCKS.registerMetal("soulsteel", metalBuilder(MapColor.BROWN).nonOpaque(), HIDDEN_BLOCK_ITEM, HIDDEN_PROPS);
    public static final ItemObject<Item> copperNugget = ITEMS.register("copper_nugget", GENERAL_PROPS);
    public static final ItemObject<Item> netheriteNugget = ITEMS.register("netherite_nugget", GENERAL_PROPS);
    public static final ItemObject<Item> debrisNugget = ITEMS.register("debris_nugget", TOOLTIP_ITEM);
    // tier 5
    public static final MetalItemObject knightslime = BLOCKS.registerMetal("knightslime", metalBuilder(MapColor.MAGENTA), HIDDEN_BLOCK_ITEM, HIDDEN_PROPS);

    // non-metal
    public static final ItemObject<Item> necroticBone = ITEMS.register("necrotic_bone", TOOLTIP_ITEM);
    public static final ItemObject<Item> venombone = ITEMS.register("venombone", TOOLTIP_ITEM);
    public static final ItemObject<Item> blazingBone = ITEMS.register("blazing_bone", TOOLTIP_ITEM);
    public static final ItemObject<Item> necroniumBone = ITEMS.register("necronium_bone", TOOLTIP_ITEM);
    public static final FenceBuildingBlockObject nahuatl = BLOCKS.registerFenceBuilding("nahuatl", builder(MapColor.SPRUCE_BROWN, BlockSoundGroup.WOOD).requiresTool().strength(25f, 300f), GENERAL_BLOCK_ITEM);
    public static final FenceBuildingBlockObject blazewood = BLOCKS.registerFenceBuilding("blazewood", woodBuilder(MapColor.TERRACOTTA_RED).requiresTool().strength(25f, 300f).luminance(s -> 7), GENERAL_BLOCK_ITEM);

    /*
     * Serializers
     */
//    @SubscribeEvent
//    void registerSerializers(RegisterEvent event) {
//        if (event.getRegistryKey() == Registry.RECIPE_SERIALIZER_REGISTRY) {
//            CraftingHelper.register(MaterialIngredient.Serializer.ID, MaterialIngredient.Serializer.INSTANCE);
//        }
//    }
}
