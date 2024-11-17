package slimeknights.tconstruct.library.tools.item;

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
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DurabilityDisplayModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningSpeedToolHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerToolActions;

import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A standard modifiable item which implements melee hooks
 * This class handles how all the modifier hooks and display data for items made out of different materials
 */
public class ModifiableItem extends Item implements IModifiableDisplay {
    /**
     * Tool definition for the given tool
     */
    @Getter
    private final ToolDefinition toolDefinition;

    /**
     * Cached tool for rendering on UIs
     */
    private ItemStack toolForRendering;

    public ModifiableItem(Settings properties, ToolDefinition toolDefinition) {
        super(properties);
        this.toolDefinition = toolDefinition;
    }


    /* Basic properties */

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
    public boolean isItemBarVisible(ItemStack stack) {
        return stack.getCount() == 1 && DurabilityDisplayModifierHook.showDurabilityBar(stack);
    }

    @Override
    public int getItemBarColor(ItemStack pStack) {
        return DurabilityDisplayModifierHook.getDurabilityRGB(pStack);
    }

    @Override
    public int getItemBarStep(ItemStack pStack) {
        return DurabilityDisplayModifierHook.getDurabilityWidth(pStack);
    }


    /* Attacking */

    @Override
    public boolean onLeftClickEntity(ItemStack stack, PlayerEntity player, Entity target) {
        return stack.getCount() > 1 || EntityInteractionModifierHook.leftClickEntity(stack, player, target);
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


    /* Harvest logic */

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
        return stack.getCount() == 1 ? MiningSpeedToolHook.getDestroySpeed(stack, state) : 0;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, PlayerEntity player) {
        return stack.getCount() > 1 || ToolHarvestLogic.handleBlockBreak(stack, pos, player);
    }


    /* Modifier interactions */

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        InventoryTickModifierHook.heldInventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
    }

    /* Right click hooks */

    /**
     * If true, this interaction hook should defer to the offhand
     */
    protected static boolean shouldInteract(@Nullable LivingEntity player, ToolStack toolStack, Hand hand) {
        IModDataView volatileData = toolStack.getVolatileData();
        if (volatileData.getBoolean(NO_INTERACTION)) {
            return false;
        }
        // off hand always can interact
        if (hand == Hand.OFF_HAND) {
            return true;
        }
        // main hand may wish to defer to the offhand if it has a tool
        return player == null || !volatileData.getBoolean(DEFER_OFFHAND) || player.getOffHandStack().isEmpty();
    }

    @Override
    public ActionResult onItemUseFirst(ItemStack stack, ItemUsageContext context) {
        if (stack.getCount() == 1) {
            ToolStack tool = ToolStack.from(stack);
            Hand hand = context.getHand();
            if (shouldInteract(context.getPlayer(), tool, hand)) {
                for (ModifierEntry entry : tool.getModifierList()) {
                    ActionResult result = entry.getHook(ModifierHooks.BLOCK_INTERACT).beforeBlockUse(tool, entry, context, InteractionSource.RIGHT_CLICK);
                    if (result.isAccepted()) {
                        return result;
                    }
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        if (stack.getCount() == 1) {
            ToolStack tool = ToolStack.from(stack);
            Hand hand = context.getHand();
            if (shouldInteract(context.getPlayer(), tool, hand)) {
                for (ModifierEntry entry : tool.getModifierList()) {
                    ActionResult result = entry.getHook(ModifierHooks.BLOCK_INTERACT).afterBlockUse(tool, entry, context, InteractionSource.RIGHT_CLICK);
                    if (result.isAccepted()) {
                        return result;
                    }
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        ToolStack tool = ToolStack.from(stack);
        if (shouldInteract(playerIn, tool, hand)) {
            for (ModifierEntry entry : tool.getModifierList()) {
                ActionResult result = entry.getHook(ModifierHooks.ENTITY_INTERACT).afterEntityUse(tool, entry, playerIn, target, hand, InteractionSource.RIGHT_CLICK);
                if (result.isAccepted()) {
                    return result;
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand hand) {
        ItemStack stack = playerIn.getStackInHand(hand);
        if (stack.getCount() > 1) {
            return TypedActionResult.pass(stack);
        }
        ToolStack tool = ToolStack.from(stack);
        if (shouldInteract(playerIn, tool, hand)) {
            for (ModifierEntry entry : tool.getModifierList()) {
                ActionResult result = entry.getHook(ModifierHooks.GENERAL_INTERACT).onToolUse(tool, entry, playerIn, hand, InteractionSource.RIGHT_CLICK);
                if (result.isAccepted()) {
                    return new TypedActionResult<>(result, stack);
                }
            }
        }
        return new TypedActionResult<>(ToolInventoryCapability.tryOpenContainer(stack, tool, playerIn, Util.getSlotType(hand)), stack);
    }

    @Override
    public void usageTick(World pLevel, LivingEntity entityLiving, ItemStack stack, int timeLeft) {
        ToolStack tool = ToolStack.from(stack);
        ModifierEntry activeModifier = GeneralInteractionModifierHook.getActiveModifier(tool);
        if (activeModifier != ModifierEntry.EMPTY) {
            activeModifier.getHook(ModifierHooks.GENERAL_INTERACT).onUsingTick(tool, activeModifier, entityLiving, timeLeft);
        }
    }

    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        if (super.canContinueUsing(oldStack, newStack)) {
            if (oldStack != newStack) {
                GeneralInteractionModifierHook.finishUsing(ToolStack.from(oldStack));
            }
        }
        return super.canContinueUsing(oldStack, newStack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        ToolStack tool = ToolStack.from(stack);
        ModifierEntry activeModifier = GeneralInteractionModifierHook.getActiveModifier(tool);
        if (activeModifier != ModifierEntry.EMPTY) {
            activeModifier.getHook(ModifierHooks.GENERAL_INTERACT).onFinishUsing(tool, activeModifier, entityLiving);
        }
        GeneralInteractionModifierHook.finishUsing(tool);
        return stack;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World worldIn, LivingEntity entityLiving, int timeLeft) {
        ToolStack tool = ToolStack.from(stack);
        ModifierEntry activeModifier = GeneralInteractionModifierHook.getActiveModifier(tool);
        if (activeModifier != ModifierEntry.EMPTY) {
            activeModifier.getHook(ModifierHooks.GENERAL_INTERACT).onStoppedUsing(tool, activeModifier, entityLiving, timeLeft);
        }
        GeneralInteractionModifierHook.finishUsing(tool);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        ToolStack tool = ToolStack.from(stack);
        ModifierEntry activeModifier = GeneralInteractionModifierHook.getActiveModifier(tool);
        if (activeModifier != ModifierEntry.EMPTY) {
            return activeModifier.getHook(ModifierHooks.GENERAL_INTERACT).getUseDuration(tool, activeModifier);
        }
        return 0;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        ToolStack tool = ToolStack.from(stack);
        ModifierEntry activeModifier = GeneralInteractionModifierHook.getActiveModifier(tool);
        if (activeModifier != ModifierEntry.EMPTY) {
            return activeModifier.getHook(ModifierHooks.GENERAL_INTERACT).getUseAction(tool, activeModifier);
        }
        return UseAction.NONE;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return stack.getCount() == 1 && ModifierUtil.canPerformAction(ToolStack.from(stack), toolAction);
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

    /**
     * Logic to prevent reanimation on tools when properties such as autorepair change.
     *
     * @param oldStack    Old stack instance
     * @param newStack    New stack instance
     * @param slotChanged If true, a slot changed
     * @return True if a reequip animation should be triggered
     */
    public static boolean shouldCauseReequip(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack == newStack) {
            return false;
        }
        // basic changes
        if (slotChanged || oldStack.getItem() != newStack.getItem()) {
            return true;
        }

        // if the tool props changed,
        ToolStack oldTool = ToolStack.from(oldStack);
        ToolStack newTool = ToolStack.from(newStack);

        // check if modifiers or materials changed
        if (!oldTool.getMaterials().equals(newTool.getMaterials())) {
            return true;
        }
        if (!oldTool.getModifierList().equals(newTool.getModifierList())) {
            return true;
        }

        // if the attributes changed, reequip
        Multimap<EntityAttribute, EntityAttributeModifier> attributesNew = newStack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        Multimap<EntityAttribute, EntityAttributeModifier> attributesOld = oldStack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (attributesNew.size() != attributesOld.size()) {
            return true;
        }
        for (EntityAttribute attribute : attributesOld.keySet()) {
            if (!attributesNew.containsKey(attribute)) {
                return true;
            }
            Iterator<EntityAttributeModifier> iter1 = attributesNew.get(attribute).iterator();
            Iterator<EntityAttributeModifier> iter2 = attributesOld.get(attribute).iterator();
            while (iter1.hasNext() && iter2.hasNext()) {
                if (!iter1.next().equals(iter2.next())) {
                    return true;
                }
            }
        }
        // no changes, no reequip
        return false;
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return shouldCauseReequipAnimation(oldStack, newStack, false);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return shouldCauseReequip(oldStack, newStack, slotChanged);
    }


    /* Helpers */

    /**
     * Creates a raytrace and casts it to a BlockRayTraceResult
     *
     * @param worldIn   the world
     * @param player    the given player
     * @param fluidMode the fluid mode to use for the raytrace event
     * @return Raytrace
     */
    public static BlockHitResult blockRayTrace(World worldIn, PlayerEntity player, RaycastContext.FluidHandling fluidMode) {
        return Item.raycast(worldIn, player, fluidMode);
    }
}
