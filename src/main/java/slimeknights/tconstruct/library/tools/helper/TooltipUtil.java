package slimeknights.tconstruct.library.tools.helper;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.minecraft.registry.Registries;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolPartsHook;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IToolPart;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerModifiers;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStack.TooltipSection;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Helper functions for adding tooltips to tools
 */
public class TooltipUtil {
    /**
     * Translation key for the tool name format string
     */
    public static final String KEY_FORMAT = TConstruct.makeTranslationKey("item", "tool.format");
    /**
     * Format for a name ID pair
     */
    public static final String KEY_ID_FORMAT = TConstruct.makeTranslationKey("item", "tool.id_format");
    /**
     * Translation key for the tool name format string
     */
    private static final Text MATERIAL_SEPARATOR = TConstruct.makeTranslation("item", "tool.material_separator");

    /**
     * Tool tag to set that makes a tool a display tool
     */
    public static final String KEY_DISPLAY = "tic_display";
    /**
     * Tag to set name without name being italic
     */
    private static final String KEY_NAME = "tic_name";

    /**
     * Function to show all attributes in the tooltip
     */
    public static final BiPredicate<EntityAttribute, Operation> SHOW_ALL_ATTRIBUTES = (att, op) -> true;
    /**
     * Function to show all attributes in the tooltip
     */
    public static final BiPredicate<EntityAttribute, Operation> SHOW_MELEE_ATTRIBUTES = (att, op) -> op != Operation.ADDITION || (att != EntityAttributes.GENERIC_ATTACK_DAMAGE && att != EntityAttributes.GENERIC_ATTACK_SPEED && att != EntityAttributes.GENERIC_ARMOR && att != EntityAttributes.GENERIC_ARMOR_TOUGHNESS && att != EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
    /**
     * Function to show all attributes in the tooltip
     */
    public static final BiPredicate<EntityAttribute, Operation> SHOW_ARMOR_ATTRIBUTES = (att, op) -> op != Operation.ADDITION || (att != EntityAttributes.GENERIC_ARMOR && att != EntityAttributes.GENERIC_ARMOR_TOUGHNESS && att != EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);

    /**
     * Flags used when not holding control or shift
     */
    private static final int DEFAULT_HIDE_FLAGS = TooltipSection.ENCHANTMENTS.getFlag();
    /**
     * Flags used when holding control or shift
     */
    private static final int MODIFIER_HIDE_FLAGS = TooltipSection.ENCHANTMENTS.getFlag() | TooltipSection.MODIFIERS.getFlag();

    private TooltipUtil() {
    }

    /**
     * Tooltip telling the player to hold shift for more info
     */
    public static final Text TOOLTIP_HOLD_SHIFT = TConstruct.makeTranslation("tooltip", "hold_shift", TConstruct.makeTranslation("key", "shift").formatted(Formatting.YELLOW, Formatting.ITALIC));
    /**
     * Tooltip telling the player to hold control for part info
     */
    public static final Text TOOLTIP_HOLD_CTRL = TConstruct.makeTranslation("tooltip", "hold_ctrl", TConstruct.makeTranslation("key", "ctrl").formatted(Formatting.AQUA, Formatting.ITALIC));
    /**
     * Tooltip for when tool data is missing
     */
    private static final Text NO_DATA = TConstruct.makeTranslation("tooltip", "missing_data").formatted(Formatting.GRAY);
    /**
     * Tooltip for when a tool is uninitialized
     */
    private static final Text UNINITIALIZED = TConstruct.makeTranslation("tooltip", "uninitialized").formatted(Formatting.GRAY);
    /**
     * Extra tooltip for multipart tools with no materials
     */
    private static final Text RANDOM_MATERIALS = TConstruct.makeTranslation("tooltip", "random_materials").formatted(Formatting.GRAY);

    /**
     * If true, this stack was created for display, so some of the tooltip is suppressed
     *
     * @param stack Stack to check
     * @return True if marked display
     */
    public static boolean isDisplay(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(KEY_DISPLAY);
    }

    /**
     * Gets the name for a given material variant
     */
    @Nullable
    private static Text nameFor(String itemKey, Text itemName, MaterialVariantId variantId) {
        String materialKey = MaterialTooltipCache.getKey(variantId);
        String key = itemKey + "." + materialKey;
        if (Util.canTranslate(key)) {
            return Text.translatable(key);
        }
        // name format override
        String formatKey = materialKey + ".format";
        if (Util.canTranslate(formatKey)) {
            return Text.translatable(formatKey, itemName);
        }
        // base name with generic format
        if (Util.canTranslate(materialKey)) {
            return Text.translatable(KEY_FORMAT, Text.translatable(materialKey), itemName);
        }
        return null;
    }

    /**
     * Gets the display name for a single material
     *
     * @param stack    Stack instance
     * @param itemName Name of the stack on its own
     * @param material Material to use
     * @return Name for a material tool
     */
    private static Text getMaterialItemName(ItemStack stack, Text itemName, MaterialVariantId material) {
        String itemKey = stack.getTranslationKey();
        if (material.hasVariant()) {
            Text component = nameFor(itemKey, itemName, material);
            if (component != null) {
                return component;
            }
        }
        Text component = nameFor(itemKey, itemName, material.getId());
        if (component != null) {
            return component;
        }
        return itemName;
    }

    /**
     * Combines the given display name with the material names to form the new given name
     *
     * @param itemName  the standard display name
     * @param materials the list of material names
     * @return the combined item name
     */
    private static Text getCombinedItemName(Text itemName, Collection<Text> materials) {
        if (materials.isEmpty()) {
            return itemName;
        }
        // separate materials by dash
        MutableText name = Text.literal("");
        Iterator<Text> iter = materials.iterator();
        name.append(iter.next());
        while (iter.hasNext()) {
            name.append(MATERIAL_SEPARATOR).append(iter.next());
        }
        return Text.translatable(KEY_FORMAT, name, itemName);
    }

    /**
     * Sets the tool name in a way that will not be italic
     */
    public static void setDisplayName(ItemStack tool, String name) {
        if (name.isEmpty()) {
            NbtCompound tag = tool.getNbt();
            if (tag != null) {
                tag.remove(KEY_NAME);
            }
        } else {
            tool.getOrCreateNbt().putString(KEY_NAME, name);
        }
        tool.removeCustomName();
    }

    /**
     * Gets the display name from the given tool
     */
    public static String getDisplayName(ItemStack tool) {
        NbtCompound tag = tool.getNbt();
        if (tag != null) {
            return tag.getString(KEY_NAME);
        }
        return "";
    }

    /**
     * Gets the display name for a tool including the head material in the name
     *
     * @param stack          Stack instance
     * @param toolDefinition Tool definition
     * @return Display name including the head material
     */
    public static Text getDisplayName(ItemStack stack, ToolDefinition toolDefinition) {
        return getDisplayName(stack, null, toolDefinition);
    }

    /**
     * Gets the display name for a tool including the head material in the name
     *
     * @param stack Stack instance
     * @param tool  Tool instance
     * @return Display name including the head material
     */
    public static Text getDisplayName(ItemStack stack, @Nullable IToolStackView tool, ToolDefinition toolDefinition) {
        String name = getDisplayName(stack);
        if (!name.isEmpty()) {
            return Text.literal(name);
        }
        List<MaterialStatsId> components = ToolMaterialHook.stats(toolDefinition);
        Text baseName = Text.translatable(stack.getTranslationKey());
        if (components.isEmpty()) {
            return baseName;
        }

        // if there is a mismatch in material size, just stop here
        if (tool == null) tool = ToolStack.from(stack);
        MaterialNBT materials = tool.getMaterials();
        if (materials.size() != components.size()) {
            return baseName;
        }

        // if the tool is not named we use the repair materials for a prefix like thing
        // set ensures we don't use the same name twice, specifically a set of components ensures if two variants have the same name we don't use both
        Set<Text> nameMaterials = Sets.newLinkedHashSet();
        MaterialVariantId firstMaterial = null;
        IMaterialRegistry registry = MaterialRegistry.getInstance();
        for (int i = 0; i < components.size(); i++) {
            if (i < materials.size() && registry.canRepair(components.get(i))) {
                MaterialVariantId material = materials.get(i).getVariant();
                if (!IMaterial.UNKNOWN_ID.equals(material)) {
                    if (firstMaterial == null) {
                        firstMaterial = material;
                    }
                    nameMaterials.add(MaterialTooltipCache.getDisplayName(material));
                }
            }
        }
        // if a single material, use the single material logic
        if (nameMaterials.size() == 1) {
            return getMaterialItemName(stack, baseName, firstMaterial);
        }
        // multiple means we mix them together
        return getCombinedItemName(baseName, nameMaterials);
    }

    /**
     * Replaces the world argument with the local player
     */
    public static void addInformation(IModifiableDisplay item, ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        PlayerEntity player = world == null ? null : SafeClientAccess.getPlayer();
        TooltipUtil.addInformation(item, stack, player, tooltip, tooltipKey, tooltipFlag);
    }

    /**
     * Full logic for adding tooltip information, other than attributes
     */
    public static void addInformation(IModifiableDisplay item, ItemStack stack, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        // if the display tag is set, just show modifiers
        ToolDefinition definition = item.getToolDefinition();
        if (isDisplay(stack)) {
            ToolStack tool = ToolStack.from(stack);
            addModifierNames(stack, tool, tooltip, tooltipFlag);
            // No definition?
        } else if (!definition.isDataLoaded()) {
            tooltip.add(NO_DATA);

            // if not initialized, show no data tooltip on non-standard items
        } else if (!ToolStack.isInitialized(stack)) {
            tooltip.add(UNINITIALIZED);
            if (definition.hasMaterials()) {
                NbtCompound nbt = stack.getNbt();
                if (nbt == null || !nbt.contains(ToolStack.TAG_MATERIALS, NbtElement.LIST_TYPE)) {
                    tooltip.add(RANDOM_MATERIALS);
                }
            }
        } else {
            switch (tooltipKey) {
                case SHIFT:
                    item.getStatInformation(ToolStack.from(stack), player, tooltip, tooltipKey, tooltipFlag);
                    break;
                case CONTROL:
                    if (definition.hasMaterials()) {
                        getComponents(item, stack, tooltip, tooltipFlag);
                        break;
                    }
                    // intentional fallthrough
                default:
                    ToolStack tool = ToolStack.from(stack);
                    getDefaultInfo(stack, tool, tooltip, tooltipFlag);
                    break;
            }
        }
    }

    /**
     * Adds modifier names to the tooltip
     *
     * @param stack    Stack instance. If empty, skips adding enchantment names
     * @param tool     Tool instance
     * @param tooltips Tooltip list
     * @param flag     Tooltip flag
     */
    @SuppressWarnings("deprecation")
    public static void addModifierNames(ItemStack stack, IToolStackView tool, List<Text> tooltips, TooltipContext flag) {
        for (ModifierEntry entry : tool.getModifierList()) {
            if (entry.getModifier().shouldDisplay(false)) {
                Text name = entry.getModifier().getDisplayName(tool, entry);
                if (flag.isAdvanced() && Config.CLIENT.modifiersIDsInAdvancedTooltips.get()) {
                    tooltips.add(Text.translatable(KEY_ID_FORMAT, name, Text.literal(entry.getModifier().getId().toString())).formatted(Formatting.DARK_GRAY));
                } else {
                    tooltips.add(name);
                }
            }
        }
        if (!stack.isEmpty()) {
            NbtCompound tag = stack.getNbt();
            if (tag != null && tag.contains("Enchantments", NbtElement.LIST_TYPE)) {
                NbtList enchantments = tag.getList("Enchantments", NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < enchantments.size(); ++i) {
                    NbtCompound enchantmentTag = enchantments.getCompound(i);
                    // TODO: is this the best place for this, or should we let vanilla run?
                    Registries.ENCHANTMENT.getOrEmpty(Identifier.tryParse(enchantmentTag.getString("id")))
                            .ifPresent(enchantment -> tooltips.add(enchantment.getName(enchantmentTag.getInt("lvl"))));
                }
            }
        }
    }

    /**
     * Adds information when holding neither control nor shift
     *
     * @param tool     Tool stack instance
     * @param tooltips Tooltip list
     * @param flag     Tooltip flag
     */
    public static void getDefaultInfo(ItemStack stack, IToolStackView tool, List<Text> tooltips, TooltipContext flag) {
        // shows as broken when broken, hold shift for proper durability
        if (tool.getItem().isDamageable() && !tool.isUnbreakable() && tool.hasTag(TinkerTags.Items.DURABILITY)) {
            tooltips.add(TooltipBuilder.formatDurability(tool.getCurrentDurability(), tool.getStats().getInt(ToolStats.DURABILITY), true));
        }
        // modifier tooltip
        addModifierNames(stack, tool, tooltips, flag);
        tooltips.add(Text.empty());
        tooltips.add(TOOLTIP_HOLD_SHIFT);
        if (tool.getDefinition().hasMaterials()) {
            tooltips.add(TOOLTIP_HOLD_CTRL);
        }
    }

    /**
     * Gets the  default information for the given tool stack
     *
     * @param tool    the tool stack
     * @param tooltip Tooltip list
     * @param flag    Tooltip flag
     * @return List from the parameter after filling
     */
    public static List<Text> getDefaultStats(IToolStackView tool, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey key, TooltipContext flag) {
        TooltipBuilder builder = new TooltipBuilder(tool, tooltip);
        if (tool.hasTag(TinkerTags.Items.DURABILITY)) {
            builder.addDurability();
        }
        if (tool.hasTag(TinkerTags.Items.RANGED)) {
            builder.add(ToolStats.DRAW_SPEED);
            builder.add(ToolStats.VELOCITY);
            builder.add(ToolStats.PROJECTILE_DAMAGE);
            builder.add(ToolStats.ACCURACY);
        }
        if (tool.hasTag(TinkerTags.Items.MELEE_WEAPON)) {
            builder.addWithAttribute(ToolStats.ATTACK_DAMAGE, EntityAttributes.GENERIC_ATTACK_DAMAGE);
            builder.add(ToolStats.ATTACK_SPEED);
        }
        if (tool.hasTag(TinkerTags.Items.HARVEST)) {
            if (tool.hasTag(TinkerTags.Items.HARVEST_PRIMARY)) {
                builder.addTier();
            }
            builder.add(ToolStats.MINING_SPEED);
        }
        // slimestaffs and shields are holdable armor, so show armor stats
        if (tool.hasTag(TinkerTags.Items.ARMOR)) {
            builder.add(ToolStats.ARMOR);
            builder.addOptional(ToolStats.ARMOR_TOUGHNESS);
            builder.addOptional(ToolStats.KNOCKBACK_RESISTANCE, 10f);
        }
        // TODO: should this be a tag? or a volatile flag?
        if (tool.getModifierLevel(TinkerModifiers.blocking.getId()) > 0 || tool.getModifierLevel(TinkerModifiers.parrying.getId()) > 0) {
            builder.add(ToolStats.BLOCK_AMOUNT);
            builder.add(ToolStats.BLOCK_ANGLE);
        }

        builder.addAllFreeSlots();
        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(ModifierHooks.TOOLTIP).addTooltip(tool, entry, player, tooltip, key, flag);
        }
        return builder.getTooltips();
    }

    /**
     * Gets the  default information for the given tool stack
     *
     * @param tool    the tool stack
     * @param tooltip Tooltip list
     * @param flag    Tooltip flag
     * @return List from the parameter after filling
     */
    public static List<Text> getArmorStats(IToolStackView tool, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey key, TooltipContext flag) {
        TooltipBuilder builder = new TooltipBuilder(tool, tooltip);
        if (tool.hasTag(TinkerTags.Items.DURABILITY)) {
            builder.addDurability();
        }
        if (tool.hasTag(TinkerTags.Items.ARMOR)) {
            builder.add(ToolStats.ARMOR);
            builder.addOptional(ToolStats.ARMOR_TOUGHNESS);
            builder.addOptional(ToolStats.KNOCKBACK_RESISTANCE, 10f);
        }
        if (tool.hasTag(TinkerTags.Items.UNARMED)) {
            builder.addWithAttribute(ToolStats.ATTACK_DAMAGE, EntityAttributes.GENERIC_ATTACK_DAMAGE);
        }

        builder.addAllFreeSlots();

        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(ModifierHooks.TOOLTIP).addTooltip(tool, entry, player, tooltip, key, flag);
        }
        return builder.getTooltips();
    }

    /**
     * Gets the tooltip of the components list of a tool
     *
     * @param item     Modifiable item instance
     * @param stack    Item stack being displayed
     * @param tooltips List of tooltips
     * @param flag     Tooltip flag, if advanced will show material IDs
     */
    public static void getComponents(IModifiable item, ItemStack stack, List<Text> tooltips, TooltipContext flag) {
        // no components, nothing to do
        List<MaterialStatsId> components = ToolMaterialHook.stats(item.getToolDefinition());
        if (components.isEmpty()) {
            return;
        }
        // no materials is bad
        MaterialNBT materials = ToolStack.from(stack).getMaterials();
        if (materials.size() == 0) {
            tooltips.add(NO_DATA);
            return;
        }
        // wrong number is bad
        if (materials.size() < components.size()) {
            return;
        }
        // start by displaying all tool parts
        int max = components.size() - 1;
        List<IToolPart> parts = ToolPartsHook.parts(item.getToolDefinition());
        int partCount = parts.size();
        for (int i = 0; i <= max; i++) {
            MaterialVariantId material = materials.get(i).getVariant();
            // display tool parts as the tool part name, nicer to work with
            Text componentName;
            if (i < partCount) {
                componentName = parts.get(i).withMaterial(material).getName();
            } else {
                componentName = MaterialTooltipCache.getDisplayName(material);
            }
            // underline it and color it with the material name
            tooltips.add(componentName.copy().formatted(Formatting.UNDERLINE).styled(style -> style.withColor(MaterialTooltipCache.getColor(material))));
            // material IDs on advanced
            if (flag.isAdvanced()) {
                tooltips.add((Text.literal(material.toString())).formatted(Formatting.DARK_GRAY));
            }
            // material stats
            MaterialRegistry.getInstance().getMaterialStats(material.getId(), components.get(i)).ifPresent(stat -> tooltips.addAll(stat.getLocalizedInfo()));
            if (i != max) {
                tooltips.add(Text.empty());
            }
        }
    }

    /**
     * Adds attributes to the tooltip
     *
     * @param item          Modifiable item instance
     * @param tool          Tool instance, primary source of info for the tool
     * @param player        Player instance
     * @param tooltip       Tooltip instance
     * @param showAttribute Predicate to determine whether an attribute should show
     * @param slots         List of slots to display
     */
    public static void addAttributes(ITinkerStationDisplay item, IToolStackView tool, @Nullable PlayerEntity player, List<Text> tooltip, BiPredicate<EntityAttribute, Operation> showAttribute, EquipmentSlot... slots) {
        for (EquipmentSlot slot : slots) {
            Multimap<EntityAttribute, EntityAttributeModifier> modifiers = item.getAttributeModifiers(tool, slot);
            if (!modifiers.isEmpty()) {
                if (slots.length > 1) {
                    tooltip.add(Text.empty());
                    tooltip.add((Text.translatable("item.modifiers." + slot.getName())).formatted(Formatting.GRAY));
                }

                for (Entry<EntityAttribute, EntityAttributeModifier> entry : modifiers.entries()) {
                    EntityAttribute attribute = entry.getKey();
                    EntityAttributeModifier modifier = entry.getValue();
                    Operation operation = modifier.getOperation();
                    // allow suppressing specific attributes
                    if (!showAttribute.test(attribute, operation)) {
                        continue;
                    }
                    // find value
                    double amount = modifier.getValue();
                    boolean showEquals = false;
                    if (player != null) {
                        if (modifier.getId() == Item.ATTACK_DAMAGE_MODIFIER_ID) {
                            amount += player.getAttributeBaseValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                            showEquals = true;
                        } else if (modifier.getId() == Item.ATTACK_SPEED_MODIFIER_ID) {
                            amount += player.getAttributeBaseValue(EntityAttributes.GENERIC_ATTACK_SPEED);
                            showEquals = true;
                        }
                    }
                    // some numbers display a bit different
                    double displayValue = amount;
                    if (modifier.getOperation() == Operation.ADDITION) {
                        // vanilla multiplies knockback resist by 10 for some odd reason
                        if (attribute.equals(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)) {
                            displayValue *= 10;
                        }
                    } else {
                        // display multiply as percentage
                        displayValue *= 100;
                    }
                    // final tooltip addition
                    Text name = Text.translatable(attribute.getTranslationKey());
                    if (showEquals) {
                        tooltip.add(Text.literal(" ")
                                .append(Text.translatable("attribute.modifier.equals." + operation.getId(), ItemStack.MODIFIER_FORMAT.format(displayValue), name))
                                .formatted(Formatting.DARK_GREEN));
                    } else if (amount > 0.0D) {
                        tooltip.add((Text.translatable("attribute.modifier.plus." + operation.getId(), ItemStack.MODIFIER_FORMAT.format(displayValue), name))
                                .formatted(Formatting.BLUE));
                    } else if (amount < 0.0D) {
                        displayValue *= -1;
                        tooltip.add((Text.translatable("attribute.modifier.take." + operation.getId(), ItemStack.MODIFIER_FORMAT.format(displayValue), name))
                                .formatted(Formatting.RED));
                    }
                }
            }
        }
    }

    /**
     * Gets the tooltip flags for the current ctrl+shift combination, used to hide enchantments and modifiers from the tooltip as needed
     */
    public static int getModifierHideFlags(ToolDefinition definition) {
        TooltipKey key = SafeClientAccess.getTooltipKey();
        if (key == TooltipKey.SHIFT || (key == TooltipKey.CONTROL && definition.hasMaterials())) {
            return MODIFIER_HIDE_FLAGS;
        }
        return DEFAULT_HIDE_FLAGS;
    }
}
