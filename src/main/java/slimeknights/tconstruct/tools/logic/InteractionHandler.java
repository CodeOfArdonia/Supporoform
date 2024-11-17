package slimeknights.tconstruct.tools.logic;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;
import java.util.function.Function;

/**
 * This class handles interaction based event hooks
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Bus.FORGE)
public class InteractionHandler {
    public static final EquipmentSlot[] HAND_SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    /**
     * Implements {@link EntityInteractionModifierHook#beforeEntityUse(IToolStackView, ModifierEntry, PlayerEntity, Entity, Hand, InteractionSource)}
     */
    @SubscribeEvent
    static void beforeEntityInteract(EntityInteract event) {
        ItemStack stack = event.getItemStack();
        PlayerEntity player = event.getEntity();
        Hand hand = event.getHand();
        InteractionSource source = InteractionSource.RIGHT_CLICK;
        if (!stack.isIn(TinkerTags.Items.HELD)) {
            // if the hand is empty, allow performing chestplate interaction (assuming a modifiable chestplate)
            if (stack.isEmpty()) {
                stack = player.getEquippedStack(EquipmentSlot.CHEST);
                if (stack.isIn(TinkerTags.Items.INTERACTABLE_ARMOR)) {
                    source = InteractionSource.ARMOR;
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        if (!player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            // actual interaction hook
            ToolStack tool = ToolStack.from(stack);
            Entity target = event.getTarget();
            for (ModifierEntry entry : tool.getModifierList()) {
                // exit on first successful result
                ActionResult result = entry.getHook(ModifierHooks.ENTITY_INTERACT).beforeEntityUse(tool, entry, player, target, hand, source);
                if (result.isAccepted()) {
                    event.setCanceled(true);
                    event.setCancellationResult(result);
                    return;
                }
            }
        }
    }

    /**
     * Implements {@link EntityInteractionModifierHook#afterEntityUse(IToolStackView, ModifierEntry, PlayerEntity, LivingEntity, Hand, InteractionSource)} for chestplates
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void afterEntityInteract(EntityInteract event) {
        PlayerEntity player = event.getEntity();
        if (event.getItemStack().isEmpty() && !player.isSpectator()) {
            ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
            if (chestplate.isIn(TinkerTags.Items.INTERACTABLE_ARMOR) && !player.getItemCooldownManager().isCoolingDown(chestplate.getItem())) {
                // from this point on, we are taking over interaction logic, to ensure chestplate hooks run in the right order
                event.setCanceled(true);

                ToolStack tool = ToolStack.from(chestplate);
                Entity target = event.getTarget();
                Hand hand = event.getHand();

                // initial entity interaction
                ActionResult result = target.interact(player, hand);
                if (result.isAccepted()) {
                    event.setCancellationResult(result);
                    return;
                }

                // after entity use for chestplates
                if (target instanceof LivingEntity livingTarget) {
                    for (ModifierEntry entry : tool.getModifierList()) {
                        // exit on first successful result
                        result = entry.getHook(ModifierHooks.ENTITY_INTERACT).afterEntityUse(tool, entry, player, livingTarget, hand, InteractionSource.ARMOR);
                        if (result.isAccepted()) {
                            event.setCanceled(true);
                            event.setCancellationResult(result);
                            return;
                        }
                    }
                }

                // did not interact with an entity? try direct interaction
                // needs to be run here as the interact empty hook does not fire when targeting entities
                result = onChestplateUse(player, chestplate, hand);
                event.setCancellationResult(result);
            }
        }
    }

    /**
     * Runs one of the two blockUse hooks for a chestplate
     */
    private static ActionResult onBlockUse(ItemUsageContext context, IToolStackView tool, ItemStack stack, Function<ModifierEntry, ActionResult> callback) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        CachedBlockPosition info = new CachedBlockPosition(world, context.getBlockPos(), false);
        if (player != null && !player.getAbilities().allowModifyWorld && !stack.canPlaceOn(Registry.BLOCK, info)) {
            return ActionResult.PASS;
        }

        // run modifier hook
        for (ModifierEntry entry : tool.getModifierList()) {
            ActionResult result = callback.apply(entry);
            if (result.isAccepted()) {
                if (player != null) {
                    player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
                }
                return result;
            }
        }
        return ActionResult.PASS;
    }

    /**
     * Implements modifier hooks for a chestplate right clicking a block with an empty hand
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void chestplateInteractWithBlock(PlayerInteractEvent.RightClickBlock event) {
        // only handle chestplate interacts if the current hand is empty
        PlayerEntity player = event.getEntity();
        if (event.getItemStack().isEmpty() && !player.isSpectator()) {
            // item must be a chestplate
            ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
            if (chestplate.isIn(TinkerTags.Items.INTERACTABLE_ARMOR) && !player.getItemCooldownManager().isCoolingDown(chestplate.getItem())) {
                // no turning back, from this point we are fully in charge of interaction logic (since we need to ensure order of the hooks)

                // begin interaction
                ToolStack tool = ToolStack.from(chestplate);
                Hand hand = event.getHand();
                BlockHitResult trace = event.getHitVec();
                ItemUsageContext context = new ItemUsageContext(player, hand, trace);

                // first, before block use (in forge, onItemUseFirst)
                if (event.getUseItem() != Result.DENY) {
                    ActionResult result = onBlockUse(context, tool, chestplate, entry -> entry.getHook(ModifierHooks.BLOCK_INTERACT).beforeBlockUse(tool, entry, context, InteractionSource.ARMOR));
                    if (result.isAccepted()) {
                        event.setCanceled(true);
                        event.setCancellationResult(result);
                        return;
                    }
                }

                // next, block interaction
                // empty stack automatically bypasses sneak, so no need to check the hand we interacted with, just need to check the other hand
                BlockPos pos = event.getPos();
                Result useBlock = event.getUseBlock();
                if (useBlock == Result.ALLOW || (useBlock != Result.DENY
                        && (!player.shouldCancelInteraction() || player.getStackInHand(Util.getOpposite(hand)).doesSneakBypassUse(player.getLevel(), pos, player)))) {
                    ActionResult result = player.world.getBlockState(pos).use(player.world, player, hand, trace);
                    if (result.isAccepted()) {
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, ItemStack.EMPTY);
                        }
                        event.setCanceled(true);
                        event.setCancellationResult(result);
                        return;
                    }
                }

                // regular item interaction: must not be deny, and either be allow or not have a cooldown
                Result useItem = event.getUseItem();
                event.setCancellationResult(ActionResult.PASS);
                if (useItem != Result.DENY && (useItem == Result.ALLOW || !player.getItemCooldownManager().isCoolingDown(chestplate.getItem()))) {
                    // finally, after block use (in forge, onItemUse)
                    ActionResult result = onBlockUse(context, tool, chestplate, entry -> entry.getHook(ModifierHooks.BLOCK_INTERACT).afterBlockUse(tool, entry, context, InteractionSource.ARMOR));
                    if (result.isAccepted()) {
                        event.setCanceled(true);
                        event.setCancellationResult(result);
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, ItemStack.EMPTY);
                        }
                        return;
                    }
                }

                // did not interact with an entity? try direct interaction
                // needs to be run here as the interact empty hook does not fire when targeting blocks
                ActionResult result = onChestplateUse(player, chestplate, hand);
                event.setCanceled(true);
                event.setCancellationResult(result);
            }
        }
    }

    /**
     * Implements {@link GeneralInteractionModifierHook#onToolUse(IToolStackView, ModifierEntry, PlayerEntity, Hand, InteractionSource)}, called differently on client and server
     */
    public static ActionResult onChestplateUse(PlayerEntity player, ItemStack chestplate, Hand hand) {
        if (player.getItemCooldownManager().isCoolingDown(chestplate.getItem())) {
            return ActionResult.PASS;
        }

        // first, run the modifier hook
        ToolStack tool = ToolStack.from(chestplate);
        for (ModifierEntry entry : tool.getModifierList()) {
            ActionResult result = entry.getHook(ModifierHooks.GENERAL_INTERACT).onToolUse(tool, entry, player, hand, InteractionSource.ARMOR);
            if (result.isAccepted()) {
                return result;
            }
        }
        return ActionResult.PASS;
    }

    /**
     * Handles attacking using the chestplate
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onChestplateAttack(AttackEntityEvent event) {
        // Carry On is dumb and fires the attack entity event when they are not attacking entities, causing us to punch instead
        // they should not be doing that, but the author has not done anything to fix it, so just use a hacky check
        if (event.getClass() == AttackEntityEvent.class) {
            PlayerEntity attacker = event.getEntity();
            if (attacker.getMainHandStack().isEmpty()) {
                ItemStack chestplate = attacker.getEquippedStack(EquipmentSlot.CHEST);
                if (chestplate.isIn(TinkerTags.Items.UNARMED)) {
                    ToolStack tool = ToolStack.from(chestplate);
                    if (!tool.isBroken()) {
                        ToolAttackUtil.attackEntity(tool, attacker, Hand.MAIN_HAND, event.getTarget(), ToolAttackUtil.getCooldownFunction(attacker, Hand.MAIN_HAND), false, EquipmentSlot.CHEST);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    /**
     * Handles interaction from a helmet
     *
     * @param player Player instance
     * @return true if the player has a modifiable helmet
     */
    public static boolean startArmorInteract(PlayerEntity player, EquipmentSlot slotType, TooltipKey modifierKey) {
        if (!player.isSpectator()) {
            ItemStack helmet = player.getEquippedStack(slotType);
            if (helmet.isIn(TinkerTags.Items.ARMOR)) {
                ToolStack tool = ToolStack.from(helmet);
                for (ModifierEntry entry : tool.getModifierList()) {
                    if (entry.getHook(ModifierHooks.ARMOR_INTERACT).startInteract(tool, entry, player, slotType, modifierKey)) {
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Notifies modifiers the helmet keybind was released
     *
     * @param player Player instance
     * @return true if the player has a modifiable helmet
     */
    public static boolean stopArmorInteract(PlayerEntity player, EquipmentSlot slotType) {
        if (!player.isSpectator()) {
            ItemStack helmet = player.getEquippedStack(slotType);
            if (helmet.isIn(TinkerTags.Items.ARMOR)) {
                ToolStack tool = ToolStack.from(helmet);
                for (ModifierEntry entry : tool.getModifierList()) {
                    entry.getHook(ModifierHooks.ARMOR_INTERACT).stopInteract(tool, entry, player, slotType);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Runs the left click interaction for left click
     */
    private static ActionResult onLeftClickInteraction(IToolStackView tool, PlayerEntity player, Hand hand) {
        for (ModifierEntry entry : tool.getModifierList()) {
            ActionResult result = entry.getHook(ModifierHooks.GENERAL_INTERACT).onToolUse(tool, entry, player, hand, InteractionSource.LEFT_CLICK);
            if (result.isAccepted()) {
                return result;
            }
        }
        return ActionResult.PASS;
    }

    /**
     * Runs the left click interaction for left click
     */
    public static ActionResult onLeftClickInteraction(PlayerEntity player, ItemStack held, Hand hand) {
        if (player.getItemCooldownManager().isCoolingDown(held.getItem())) {
            return ActionResult.PASS;
        }
        return onLeftClickInteraction(ToolStack.from(held), player, hand);
    }

    /**
     * Sets the event result and swings the hand
     */
    private static void setLeftClickEventResult(PlayerInteractEvent event, ActionResult result) {
        if (result.isAccepted()) {
            // success means swing hand
            if (result == ActionResult.SUCCESS) {
                event.getEntity().swing(event.getHand());
            }
            event.setCancellationResult(result);
            // don't cancel the result in survival as it does not actually prevent breaking the block, just causes really weird desyncs
            // leaving uncanceled lets us still do blocky stuff but if you hold click it digs
            if (event.getEntity().getAbilities().instabuild) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Simple class to track the last tick
     */
    private static class LastTick {
        private long lastTick = 0;

        /**
         * Attempts to update the given player
         *
         * @return True if we are ready to interact again
         */
        private boolean update(PlayerEntity player) {
            if (player.age >= this.lastTick + 4) {
                this.lastTick = player.age;
                return true;
            }
            return false;
        }
    }

    /**
     * Key for the tick tracker instance
     */
    private static final ComputableDataKey<LastTick> LAST_TICK = TConstruct.createKey("last_tick", LastTick::new);

    /**
     * Implements {@link slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook} for weapons with left click
     */
    @SubscribeEvent
    static void leftClickBlock(LeftClickBlock event) {
        // ensure we have not fired this tick
        PlayerEntity player = event.getEntity();
        if (player.getCapability(TinkerDataCapability.CAPABILITY).filter(data -> data.computeIfAbsent(LAST_TICK).update(player)).isEmpty()) {
            return;
        }
        // must support interaction
        ItemStack stack = event.getItemStack();
        if (!stack.isIn(TinkerTags.Items.INTERACTABLE_LEFT) || player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            return;
        }

        // build usage context
        Hand hand = event.getHand();
        BlockPos pos = event.getPos();
        Direction direction = event.getFace();
        if (direction == null) {
            direction = player.getHorizontalFacing().getOpposite();
        }
        ItemUsageContext context = new ItemUsageContext(player, hand, new BlockHitResult(Util.toHitVec(pos, direction), direction, pos, false));

        // run modifier hooks
        ToolStack tool = ToolStack.from(stack);
        List<ModifierEntry> modifiers = tool.getModifierList();
        for (ModifierEntry entry : modifiers) {
            ActionResult result = entry.getHook(ModifierHooks.BLOCK_INTERACT).beforeBlockUse(tool, entry, context, InteractionSource.LEFT_CLICK);
            if (result.isAccepted()) {
                setLeftClickEventResult(event, result);
                // always cancel block interaction, prevents breaking glows/fires
                event.setCanceled(true);
                return;
            }
        }
        // TODO: don't think there is an equivalence to block interactions
        for (ModifierEntry entry : modifiers) {
            ActionResult result = entry.getHook(ModifierHooks.BLOCK_INTERACT).afterBlockUse(tool, entry, context, InteractionSource.LEFT_CLICK);
            if (result.isAccepted()) {
                setLeftClickEventResult(event, result);
                // always cancel block interaction, prevents breaking glows/fires
                event.setCanceled(true);
                return;
            }
        }

        // fallback to default interaction
        ActionResult result = onLeftClickInteraction(tool, player, hand);
        if (result.isAccepted()) {
            setLeftClickEventResult(event, result);
        }
    }

    /**
     * Checks if the shield block angle allows blocking this attack
     */
    public static boolean canBlock(LivingEntity holder, @Nullable Vec3d sourcePosition, IToolStackView tool) {
        // source position should never be null (checked by livingentity) but safety as its marked nullable
        if (sourcePosition == null) {
            return false;
        }
        // divide by 2 as the stat is 0 to 180 (more intutive) but logic is 0 to 90 (simplier to work with)
        // we could potentially do a quick exit here, but that would mean this method is not applicable for modifiers like reflection
        // that skip the vanilla check first
        float blockAngle = ConditionalStatModifierHook.getModifiedStat(tool, holder, ToolStats.BLOCK_ANGLE) / 2;

        // want the angle between the view vector and the
        Vec3d viewVector = holder.getRotationVec(1.0f);
        Vec3d entityPosition = holder.getPos();
        Vec3d direction = new Vec3d(entityPosition.x - sourcePosition.x, 0, entityPosition.z - sourcePosition.z);
        double length = viewVector.length() * direction.length();
        // prevent zero vector from messing with us
        if (length < 1.0E-4D) {
            return false;
        }
        // acos will return between 90 and 270, we want an absolute angle from 0 to 180
        double angle = Math.abs(180 - Math.acos(direction.dotProduct(viewVector) / length) * MathHelper.DEGREES_PER_RADIAN);
        return blockAngle >= angle;
    }

    /**
     * Implements shield stats
     */
    @SubscribeEvent
    static void onBlock(ShieldBlockEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack activeStack = entity.getActiveItem();
        if (!activeStack.isEmpty() && activeStack.isIn(TinkerTags.Items.MODIFIABLE)) {
            ToolStack tool = ToolStack.from(activeStack);
            // first check block angle
            if (!tool.isBroken() && canBlock(event.getEntity(), event.getDamageSource().getSourcePosition(), tool)) {
                // TODO: hook for conditioning block amount based on on damage type
                event.setBlockedDamage(Math.min(event.getBlockedDamage(), tool.getStats().get(ToolStats.BLOCK_AMOUNT)));
                // TODO: consider handling the item damage ourself
            } else {
                event.setCanceled(true);
            }
        }
    }
}
