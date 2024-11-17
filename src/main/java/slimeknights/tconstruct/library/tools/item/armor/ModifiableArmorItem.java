package slimeknights.tconstruct.library.tools.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DurabilityDisplayModifierHook;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ModifiableArmorItem extends ArmorItem implements IModifiableDisplay {
    /**
     * Volatile modifier tag to make piglins neutal when worn
     */
    public static final Identifier PIGLIN_NEUTRAL = TConstruct.getResource("piglin_neutral");
    /**
     * Volatile modifier tag to make this item an elytra
     */
    public static final Identifier ELYTRA = TConstruct.getResource("elyta");
    /**
     * Volatile flag for a boot item to walk on powdered snow. Cold immunity is handled through a tag
     */
    public static final Identifier SNOW_BOOTS = TConstruct.getResource("snow_boots");

    @Getter
    private final ToolDefinition toolDefinition;
    /**
     * Cache of the tool built for rendering
     */
    private ItemStack toolForRendering = null;

    public ModifiableArmorItem(ArmorMaterial materialIn, EquipmentSlot slot, Settings builderIn, ToolDefinition toolDefinition) {
        super(materialIn, slot, builderIn);
        this.toolDefinition = toolDefinition;
    }

    public ModifiableArmorItem(ModifiableArmorMaterial material, ArmorSlotType slotType, Settings properties) {
        this(material, slotType.getEquipmentSlot(), properties, Objects.requireNonNull(material.getArmorDefinition(slotType), "Missing tool definition for " + slotType));
    }

    /* Basic properties */

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
        return ModifierUtil.checkVolatileFlag(stack, PIGLIN_NEUTRAL);
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return slot == EquipmentSlot.FEET && ModifierUtil.checkVolatileFlag(stack, SNOW_BOOTS);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ModifierUtil.canPerformAction(ToolStack.from(stack), toolAction);
    }

    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, PlayerEntity player, int inventorySlot) {
        return true;
    }


    /* Enchantments */

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.isCursed() && super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        return EnchantmentModifierHook.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public Map<Enchantment, Integer> getAllEnchantments(ItemStack stack) {
        return EnchantmentModifierHook.getAllEnchantments(stack);
    }


    /* Loading */

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NbtCompound nbt) {
        return new ToolCapabilityProvider(stack);
    }

    @Override
    public void postProcessNbt(NbtCompound nbt) {
        ToolStack.verifyTag(this, nbt, this.getToolDefinition());
    }

    @Override
    public void onCraft(ItemStack stack, World levelIn, PlayerEntity playerIn) {
        ToolStack.ensureInitialized(stack, this.getToolDefinition());
    }

    @Override
    public TypedActionResult<ItemStack> use(World levelIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getStackInHand(handIn);
        ActionResult result = ToolInventoryCapability.tryOpenContainer(stack, null, this.getToolDefinition(), playerIn, Util.getSlotType(handIn));
        if (result.isAccepted()) {
            return new TypedActionResult<>(result, stack);
        }
        return super.use(levelIn, playerIn, handIn);
    }


    /* Display */

    @Override
    public boolean hasGlint(ItemStack stack) {
        // we use enchantments to handle some modifiers, so don't glow from them
        // however, if a modifier wants to glow let them
        return ModifierUtil.checkVolatileFlag(stack, SHINY);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        int rarity = ModifierUtil.getVolatileInt(stack, RARITY);
        return Rarity.values()[MathHelper.clamp(rarity, 0, 3)];
    }


    /* Indestructible items */

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return ModifierUtil.checkVolatileFlag(stack, INDESTRUCTIBLE_ENTITY);
    }

    @Override
    public Entity createEntity(World level, Entity original, ItemStack stack) {
        if (ModifierUtil.checkVolatileFlag(stack, INDESTRUCTIBLE_ENTITY)) {
            IndestructibleItemEntity entity = new IndestructibleItemEntity(level, original.getX(), original.getY(), original.getZ(), stack);
            entity.setPickupDelayFrom(original);
            return entity;
        }
        return null;
    }


    /* Damage/Durability */

    @Override
    public boolean isRepairable(ItemStack stack) {
        // handle in the tinker station
        return false;
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        if (!this.isDamageable()) {
            return 0;
        }
        ToolStack tool = ToolStack.from(stack);
        int durability = tool.getStats().getInt(ToolStats.DURABILITY);
        // vanilla deletes tools if max damage == getDamage, so tell vanilla our max is one higher when broken
        return tool.isBroken() ? durability + 1 : durability;
    }

    @Override
    public int getDamage(ItemStack stack) {
        if (!this.isDamageable()) {
            return 0;
        }
        return ToolStack.from(stack).getDamage();
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (this.isDamageable()) {
            ToolStack.from(stack).setDamage(damage);
        }
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T damager, Consumer<T> onBroken) {
        // We basically emulate Itemstack.damageItem here. We always return 0 to skip the handling in ItemStack.
        // If we don't tools ignore our damage logic
        if (this.isDamageable() && ToolDamageUtil.damage(ToolStack.from(stack), amount, damager, stack)) {
            onBroken.accept(damager);
        }

        return 0;
    }


    /* Durability display */

    @Override
    public boolean isItemBarVisible(ItemStack pStack) {
        return DurabilityDisplayModifierHook.showDurabilityBar(pStack);
    }

    @Override
    public int getItemBarColor(ItemStack pStack) {
        return DurabilityDisplayModifierHook.getDurabilityRGB(pStack);
    }

    @Override
    public int getItemBarStep(ItemStack pStack) {
        return DurabilityDisplayModifierHook.getDurabilityWidth(pStack);
    }


    /* Armor properties */

    @Override
    public boolean canRepair(ItemStack toRepair, ItemStack repair) {
        return false;
    }


    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
        if (slot != getSlot()) {
            return ImmutableMultimap.of();
        }

        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        if (!tool.isBroken()) {
            // base stats
            StatsNBT statsNBT = tool.getStats();
            UUID uuid = ARMOR_MODIFIER_UUID_PER_SLOT[slot.getEntitySlotId()];
            builder.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(uuid, "tconstruct.armor.armor", statsNBT.get(ToolStats.ARMOR), EntityAttributeModifier.Operation.ADDITION));
            builder.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(uuid, "tconstruct.armor.toughness", statsNBT.get(ToolStats.ARMOR_TOUGHNESS), EntityAttributeModifier.Operation.ADDITION));
            double knockbackResistance = statsNBT.get(ToolStats.KNOCKBACK_RESISTANCE);
            if (knockbackResistance != 0) {
                builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, new EntityAttributeModifier(uuid, "tconstruct.armor.knockback_resistance", knockbackResistance, EntityAttributeModifier.Operation.ADDITION));
            }
            // grab attributes from modifiers
            BiConsumer<EntityAttribute, EntityAttributeModifier> attributeConsumer = builder::put;
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.ATTRIBUTES).addAttributes(tool, entry, slot, attributeConsumer);
            }
        }

        return builder.build();
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (slot != getSlot() || nbt == null) {
            return ImmutableMultimap.of();
        }
        return this.getAttributeModifiers(ToolStack.from(stack), slot);
    }


    /* Elytra */

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return slot == EquipmentSlot.CHEST && !ToolDamageUtil.isBroken(stack) && ModifierUtil.checkVolatileFlag(stack, ELYTRA);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (slot == EquipmentSlot.CHEST) {
            ToolStack tool = ToolStack.from(stack);
            if (!tool.isBroken()) {
                // if any modifier says stop flying, stop flying
                for (ModifierEntry entry : tool.getModifierList()) {
                    if (entry.getHook(ModifierHooks.ELYTRA_FLIGHT).elytraFlightTick(tool, entry, entity, flightTicks)) {
                        return false;
                    }
                }
                // damage the tool and keep flying
                if (!entity.getWorld().isClient && (flightTicks + 1) % 20 == 0) {
                    ToolDamageUtil.damageAnimated(tool, 1, entity, EquipmentSlot.CHEST);
                }
                return true;
            }
        }
        return false;
    }


    /* Ticking */

    @Override
    public void inventoryTick(ItemStack stack, World levelIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, levelIn, entityIn, itemSlot, isSelected);

        // don't care about non-living, they skip most tool context
        if (entityIn instanceof LivingEntity) {
            ToolStack tool = ToolStack.from(stack);
            if (!levelIn.isClient) {
                tool.ensureHasData();
            }
            List<ModifierEntry> modifiers = tool.getModifierList();
            if (!modifiers.isEmpty()) {
                LivingEntity living = (LivingEntity) entityIn;
                boolean isCorrectSlot = living.getEquippedStack(slot) == stack;
                // we pass in the stack for most custom context, but for the sake of armor its easier to tell them that this is the correct slot for effects
                for (ModifierEntry entry : modifiers) {
                    entry.getHook(ModifierHooks.INVENTORY_TICK).onInventoryTick(tool, entry, levelIn, living, itemSlot, isSelected, isCorrectSlot, stack);
                }
            }
        }
    }


    /* Tooltips */

    @Override
    public Text getName(ItemStack stack) {
        return TooltipUtil.getDisplayName(stack, this.getToolDefinition());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext flag) {
        TooltipUtil.addInformation(this, stack, level, tooltip, SafeClientAccess.getTooltipKey(), flag);
    }

    @Override
    public List<Text> getStatInformation(IToolStackView tool, @Nullable PlayerEntity player, List<Text> tooltips, TooltipKey key, TooltipContext tooltipFlag) {
        tooltips = TooltipUtil.getArmorStats(tool, player, tooltips, key, tooltipFlag);
        TooltipUtil.addAttributes(this, tool, player, tooltips, TooltipUtil.SHOW_ARMOR_ATTRIBUTES, getSlot());
        return tooltips;
    }

    @Override
    public int getDefaultTooltipHideFlags(ItemStack stack) {
        return TooltipUtil.getModifierHideFlags(this.getToolDefinition());
    }

    /* Display items */

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (this.allowedIn(group)) {
            ToolBuildHandler.addDefaultSubItems(this, items);
        }
    }

    @Override
    public ItemStack getRenderTool() {
        if (this.toolForRendering == null) {
            this.toolForRendering = ToolBuildHandler.buildToolForRendering(this, this.getToolDefinition());
        }
        return this.toolForRendering;
    }
}
