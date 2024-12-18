package slimeknights.tconstruct.tools;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.util.SupplierCreativeTab;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.tools.item.ArmorSlotType;
import slimeknights.tconstruct.tools.item.RepairKitItem;
import slimeknights.tconstruct.tools.stats.*;

import java.util.ArrayList;
import java.util.List;

public final class TinkerToolParts extends TinkerModule {
    /**
     * Tab for all tool parts
     */
    public static final ItemGroup TAB_TOOL_PARTS = new SupplierCreativeTab(TConstruct.MOD_ID, "tool_parts", () -> {
        List<IMaterial> materials = new ArrayList<>(MaterialRegistry.getInstance().getVisibleMaterials());
        if (materials.isEmpty()) {
            return new ItemStack(TinkerToolParts.pickHead);
        }
        return TinkerToolParts.pickHead.get().withMaterial(materials.get(TConstruct.RANDOM.nextInt(materials.size())).getIdentifier());
    });
    private static final Item.Settings PARTS_PROPS = new Item.Settings().tab(TAB_TOOL_PARTS);

    // repair kit, technically a head so it filters to things useful for repair
    public static final ItemObject<RepairKitItem> repairKit = ITEMS.register("repair_kit", () -> new RepairKitItem(PARTS_PROPS));

    // rock
    public static final ItemObject<ToolPartItem> pickHead = ITEMS.register("pick_head", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID));
    public static final ItemObject<ToolPartItem> hammerHead = ITEMS.register("hammer_head", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID));
    // axe
    public static final ItemObject<ToolPartItem> smallAxeHead = ITEMS.register("small_axe_head", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID));
    public static final ItemObject<ToolPartItem> broadAxeHead = ITEMS.register("broad_axe_head", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID));
    // blades
    public static final ItemObject<ToolPartItem> smallBlade = ITEMS.register("small_blade", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID));
    public static final ItemObject<ToolPartItem> broadBlade = ITEMS.register("broad_blade", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID));
    // plates
    public static final ItemObject<ToolPartItem> roundPlate = ITEMS.register("round_plate", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID));
    public static final ItemObject<ToolPartItem> largePlate = ITEMS.register("large_plate", () -> new ToolPartItem(PARTS_PROPS, HeadMaterialStats.ID));
    // bows
    public static final ItemObject<ToolPartItem> bowLimb = ITEMS.register("bow_limb", () -> new ToolPartItem(PARTS_PROPS, LimbMaterialStats.ID));
    public static final ItemObject<ToolPartItem> bowGrip = ITEMS.register("bow_grip", () -> new ToolPartItem(PARTS_PROPS, GripMaterialStats.ID));
    public static final ItemObject<ToolPartItem> bowstring = ITEMS.register("bowstring", () -> new ToolPartItem(PARTS_PROPS, StatlessMaterialStats.BOWSTRING.getIdentifier()));
    // other parts
    public static final ItemObject<ToolPartItem> toolBinding = ITEMS.register("tool_binding", () -> new ToolPartItem(PARTS_PROPS, StatlessMaterialStats.BINDING.getIdentifier()));
    public static final ItemObject<ToolPartItem> toolHandle = ITEMS.register("tool_handle", () -> new ToolPartItem(PARTS_PROPS, HandleMaterialStats.ID));
    public static final ItemObject<ToolPartItem> toughHandle = ITEMS.register("tough_handle", () -> new ToolPartItem(PARTS_PROPS, HandleMaterialStats.ID));
    // armor
    public static final EnumObject<ArmorSlotType, ToolPartItem> plating = ITEMS.registerEnum(ArmorSlotType.values(), "plating", type -> new ToolPartItem(PARTS_PROPS, PlatingMaterialStats.TYPES.get(type.getIndex()).getId()));
    public static final ItemObject<ToolPartItem> maille = ITEMS.register("maille", () -> new ToolPartItem(PARTS_PROPS, StatlessMaterialStats.MAILLE.getIdentifier()));
    public static final ItemObject<ToolPartItem> shieldCore = ITEMS.register("shield_core", () -> new ToolPartItem(PARTS_PROPS, StatlessMaterialStats.SHIELD_CORE.getIdentifier()));

}
