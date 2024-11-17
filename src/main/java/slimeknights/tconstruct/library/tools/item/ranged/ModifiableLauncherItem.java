package slimeknights.tconstruct.library.tools.item.ranged;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.UseAction;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DurabilityDisplayModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningSpeedToolHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerToolActions;
import slimeknights.tconstruct.tools.modifiers.upgrades.ranged.ScopeModifier;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook.KEY_DRAWTIME;
import static slimeknights.tconstruct.tools.modifiers.upgrades.ranged.ScopeModifier.SCOPE;

/**
 * Base class for any items that launch projectiles
 */
public abstract class ModifiableLauncherItem extends RangedWeaponItem implements IModifiableDisplay {
    /**
     * Tool definition for the given tool
     */
    @Getter
    private final ToolDefinition toolDefinition;

    /**
     * Cached tool for rendering on UIs
     */
    private ItemStack toolForRendering;

    public ModifiableLauncherItem(Settings properties, ToolDefinition toolDefinition) {
        super(properties);
        this.toolDefinition = toolDefinition;
    }


    /* Basic properties */

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, PlayerEntity player, int inventorySlot) {
        return true;
    }


    /* Enchanting */

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
    public int getEnchantability() {
        return 0;
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
        ToolStack.verifyTag(this, nbt, getToolDefinition());
    }

    @Override
    public void onCraft(ItemStack stack, World worldIn, PlayerEntity playerIn) {
        ToolStack.ensureInitialized(stack, getToolDefinition());
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
        return IndestructibleItemEntity.hasCustomEntity(stack);
    }

    @Override
    public Entity createEntity(World world, Entity original, ItemStack stack) {
        return IndestructibleItemEntity.createFrom(world, original, stack);
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
        if (!isDamageable()) {
            return 0;
        }
        ToolStack tool = ToolStack.from(stack);
        int durability = tool.getStats().getInt(ToolStats.DURABILITY);
        // vanilla deletes tools if max damage == getDamage, so tell vanilla our max is one higher when broken
        return tool.isBroken() ? durability + 1 : durability;
    }

    @Override
    public int getDamage(ItemStack stack) {
        if (!isDamageable()) {
            return 0;
        }
        return ToolStack.from(stack).getDamage();
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (isDamageable()) {
            ToolStack.from(stack).setDamage(damage);
        }
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T damager, Consumer<T> onBroken) {
        ToolDamageUtil.handleDamageItem(stack, amount, damager, onBroken);
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


    /* Modifier interactions */

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        InventoryTickModifierHook.heldInventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
    }


    /* Attacking */

    @Override
    public boolean onLeftClickEntity(ItemStack stack, PlayerEntity player, Entity target) {
        return EntityInteractionModifierHook.leftClickEntity(stack, player, target);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ModifierUtil.canPerformAction(ToolStack.from(stack), toolAction);
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
        return AttributesModifierHook.getHeldAttributeModifiers(tool, slot);
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || slot.getType() != Type.HAND) {
            return ImmutableMultimap.of();
        }
        return getAttributeModifiers(ToolStack.from(stack), slot);
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return canPerformAction(stack, TinkerToolActions.SHIELD_DISABLE);
    }


    /* Arrow logic */

    @Override
    public int getMaxUseTime(ItemStack pStack) {
        return 72000;
    }

    @Override
    public abstract UseAction getUseAction(ItemStack pStack);

    @Override
    public ItemStack finishUsing(ItemStack stack, World pLevel, LivingEntity living) {
        ScopeModifier.stopScoping(living);
        ToolStack.from(stack).getPersistentData().remove(KEY_DRAWTIME);
        return stack;
    }

    @SuppressWarnings("deprecation") // forge is being dumb here, their method is identical to the vanilla one
    @Override
    public void usageTick(World level, LivingEntity living, ItemStack bow, int chargeRemaining) {
        // play the sound at the end of loading as an indicator its loaded, texture is another indicator
        if (!level.isClient) {
            if (getMaxUseTime(bow) - chargeRemaining == ModifierUtil.getPersistentInt(bow, KEY_DRAWTIME, -1)) {
                level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.PLAYERS, 0.75F, 1.0F);
            }
        } else if (ModifierUtil.getModifierLevel(bow, TinkerModifiers.scope.getId()) > 0) {
            int chargeTime = this.getMaxUseTime(bow) - chargeRemaining;
            if (chargeTime > 0) {
                float drawtime = ModifierUtil.getPersistentInt(bow, KEY_DRAWTIME, -1);
                if (drawtime > 0) {
                    living.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).set(SCOPE, 1 - (0.6f * Math.min(chargeTime / drawtime, 1))));
                }
            }
        }
    }


    /* Tooltips */

    @Override
    public Text getName(ItemStack stack) {
        return TooltipUtil.getDisplayName(stack, getToolDefinition());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext flag) {
        TooltipUtil.addInformation(this, stack, level, tooltip, SafeClientAccess.getTooltipKey(), flag);
    }

    @Override
    public int getDefaultTooltipHideFlags(ItemStack stack) {
        return TooltipUtil.getModifierHideFlags(getToolDefinition());
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
        if (toolForRendering == null) {
            toolForRendering = ToolBuildHandler.buildToolForRendering(this, this.getToolDefinition());
        }
        return toolForRendering;
    }


    /* Misc */

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return shouldCauseReequipAnimation(oldStack, newStack, false);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return ModifiableItem.shouldCauseReequip(oldStack, newStack, slotChanged);
    }


    /* Harvest logic, mostly used by modifiers but technically would let you make a pickaxe bow */

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return IsEffectiveToolHook.isEffective(ToolStack.from(stack), state);
    }

    @Override
    public boolean postMine(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        return ToolHarvestLogic.mineBlock(stack, worldIn, state, pos, entityLiving);
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return MiningSpeedToolHook.getDestroySpeed(stack, state);
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, PlayerEntity player) {
        return ToolHarvestLogic.handleBlockBreak(stack, pos, player);
    }


    /* Multishot helper */

    /**
     * Gets the angle to fire the first arrow, each additional arrow offsets an additional 10 degrees
     */
    public static float getAngleStart(int count) {
        return -5 * (count - 1);
    }
}
