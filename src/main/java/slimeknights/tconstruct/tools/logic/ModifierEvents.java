package slimeknights.tconstruct.tools.logic;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.modules.armor.EffectImmunityModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorLevelModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorStatModule;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.helper.ModifierLootingHandler;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.data.ModifierIds;
import slimeknights.tconstruct.tools.modules.ranged.RestrictAngleModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Events to implement modifier specific behaviors, such as those defined by {@link TinkerDataKeys}. General hooks will typically be in {@link ToolEvents}
 */
@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Bus.FORGE)
public class ModifierEvents {
    /**
     * NBT key for items to preserve their slot in soulbound
     */
    private static final String SOULBOUND_SLOT = "tic_soulbound_slot";
    /**
     * Multiplier for experience drops from events
     */
    private static final TinkerDataKey<Float> PROJECTILE_EXPERIENCE = TConstruct.createKey("projectile_experience");
    /**
     * Volatile data flag making a modifier grant the tool soulbound
     */
    public static final Identifier SOULBOUND = TConstruct.getResource("soulbound");

    @SubscribeEvent
    static void onKnockback(LivingKnockBackEvent event) {
        event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
            float knockback = data.get(TinkerDataKeys.KNOCKBACK, 0f);
            if (knockback != 0) {
                // adds +20% knockback per level
                event.setStrength(event.getStrength() * (1 + knockback));
            }
            // apply crystalbound bonus
            int crystalbound = data.get(TinkerDataKeys.CRYSTALSTRIKE, 0);
            if (crystalbound > 0) {
                RestrictAngleModule.onKnockback(event, crystalbound);
            }
        });
    }

    /**
     * Reduce fall distance for fall damage
     */
    @SubscribeEvent
    static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        float boost = ArmorStatModule.getStat(entity, TinkerDataKeys.JUMP_BOOST);
        if (boost > 0) {
            event.setDistance(Math.max(event.getDistance() - boost, 0));
        }
    }

    /**
     * Called on jumping to boost the jump height of the entity
     */
    @SubscribeEvent
    public static void onLivingJump(LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        float boost = ArmorStatModule.getStat(entity, TinkerDataKeys.JUMP_BOOST);
        if (boost > 0) {
            entity.setVelocity(entity.getVelocity().add(0, boost * 0.1, 0));
        }
    }

    /**
     * Prevents effects on the entity
     */
    @SubscribeEvent
    static void isPotionApplicable(MobEffectEvent.Applicable event) {
        event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
            if (data.computeIfAbsent(EffectImmunityModule.EFFECT_IMMUNITY).contains(event.getEffectInstance().getEffect())) {
                event.setResult(Result.DENY);
            }
        });
    }

    /**
     * Called when the player dies to store the item in the original inventory
     */
    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        // if a projectile kills the target, mark the projectile level
        DamageSource source = event.getSource();
        if (source != null && source.getSource() instanceof ProjectileEntity projectile) {
            ModifierNBT modifiers = EntityModifierCapability.getOrEmpty(projectile);
            if (!modifiers.isEmpty()) {
                event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.put(PROJECTILE_EXPERIENCE, modifiers.getEntry(ModifierIds.experienced).getEffectiveLevel()));
            }
        }
        // this is the latest we can add slot markers to the items so we can return them to slots
        LivingEntity entity = event.getEntity();
        if (!entity.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY) && entity instanceof PlayerEntity player && !(player instanceof FakePlayer)) {
            // start with the hotbar, must be soulbound or soul belt
            boolean soulBelt = ArmorLevelModule.getLevel(player, TinkerDataKeys.SOUL_BELT) > 0;
            PlayerInventory inventory = player.getInventory();
            int hotbarSize = PlayerInventory.getHotbarSize();
            for (int i = 0; i < hotbarSize; i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty() && (soulBelt || ModifierUtil.checkVolatileFlag(stack, SOULBOUND))) {
                    stack.getOrCreateNbt().putInt(SOULBOUND_SLOT, i);
                }
            }
            // rest of the inventory, only check soulbound (no modifier that moves non-soulbound currently)
            // note this includes armor and offhand
            int totalSize = inventory.size();
            for (int i = hotbarSize; i < totalSize; i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty() && ModifierUtil.checkVolatileFlag(stack, SOULBOUND)) {
                    stack.getOrCreateNbt().putInt(SOULBOUND_SLOT, i);
                }
            }
        }
    }


    /* Experience */

    /**
     * Boosts the original based on the level
     *
     * @param original Original amount
     * @param bonus    Bonus percent
     * @return Boosted XP
     */
    private static int boost(int original, float bonus) {
        return (int) (original * (1 + bonus));
    }

    @SubscribeEvent
    static void beforeBlockBreak(BreakEvent event) {
        float bonus = ArmorStatModule.getStat(event.getPlayer(), TinkerDataKeys.EXPERIENCE);
        if (bonus != 0) {
            event.setExpToDrop(boost(event.getExpToDrop(), bonus));
        }
    }

    @SubscribeEvent
    static void onExperienceDrop(LivingExperienceDropEvent event) {
        // always add armor boost, unfortunately no good way to stop shield stuff here
        float armorBoost = 0;
        PlayerEntity player = event.getAttackingPlayer();
        if (player != null) {
            armorBoost = ArmorStatModule.getStat(player, TinkerDataKeys.EXPERIENCE);
        }
        // if the target was killed by an experienced arrow, use that level
        float projectileBoost = event.getEntity().getCapability(TinkerDataCapability.CAPABILITY).resolve().map(data -> data.get(PROJECTILE_EXPERIENCE)).orElse(-1f);
        if (projectileBoost > 0) {
            event.setDroppedExperience(boost(event.getDroppedExperience(), projectileBoost * 0.5f + armorBoost));
            // experienced being zero means it was our arrow but it was not modified, do not check the held item in that case
        } else if (projectileBoost != 0 && player != null) {
            // not an arrow, just use the player's experienced level
            ToolStack tool = Modifier.getHeldTool(player, ModifierLootingHandler.getLootingSlot(player));
            float boost = (tool != null ? tool.getModifier(ModifierIds.experienced).getEffectiveLevel() : 0) * 0.5f + armorBoost;
            if (boost > 0) {
                event.setDroppedExperience(boost(event.getDroppedExperience(), boost));
            }
        }
    }


    /* Soulbound */

    /**
     * Called when the player dies to store the item in the original inventory
     */
    @SubscribeEvent
    static void onPlayerDropItems(LivingDropsEvent event) {
        // only care about real players with keep inventory off
        LivingEntity entity = event.getEntity();
        if (!entity.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY) && entity instanceof PlayerEntity player && !(entity instanceof FakePlayer)) {
            Collection<ItemEntity> drops = event.getDrops();
            Iterator<ItemEntity> iter = drops.iterator();
            PlayerInventory inventory = player.getInventory();
            List<ItemEntity> takenSlot = new ArrayList<>();
            while (iter.hasNext()) {
                ItemEntity itemEntity = iter.next();
                ItemStack stack = itemEntity.getStack();
                // find items with our soulbound tag set and move them back into the inventory, will move them over later
                NbtCompound tag = stack.getNbt();
                if (tag != null && tag.contains(SOULBOUND_SLOT, NbtElement.NUMBER_TYPE)) {
                    int slot = tag.getInt(SOULBOUND_SLOT);
                    // return the tool to its requested slot if possible, remove from the drops
                    if (inventory.getStack(slot).isEmpty()) {
                        inventory.setStack(slot, stack);
                    } else {
                        // hold off on handling items that did not get the requested slot for now
                        // want to make sure they don't get in the way of items that have not yet been seen
                        takenSlot.add(itemEntity);
                    }
                    iter.remove();
                    // don't clear the tag yet, we need it one last time for player clone
                }
            }
            // handle items that did not get their requested slot last, to ensure they don't take someone else's slot while being added to a default
            for (ItemEntity itemEntity : takenSlot) {
                ItemStack stack = itemEntity.getStack();
                if (!inventory.insertStack(stack)) {
                    // last resort, somehow we just cannot put the stack anywhere, so drop it on the ground
                    // this should never happen, but better to be safe
                    // ditch the soulbound slot tag, to prevent item stacking issues
                    NbtCompound tag = stack.getNbt();
                    if (tag != null) {
                        tag.remove(SOULBOUND_SLOT);
                        if (tag.isEmpty()) {
                            stack.setNbt(null);
                        }
                    }
                    drops.add(itemEntity);
                }
            }
        }
    }

    /**
     * Called when the new player is created to fetch the soulbound item from the old
     */
    @SubscribeEvent
    static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        PlayerEntity original = event.getOriginal();
        PlayerEntity clone = event.getEntity();
        // inventory already copied
        if (clone.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || original.isSpectator()) {
            return;
        }
        // find items with the soulbound tag set and move them over
        PlayerInventory originalInv = original.getInventory();
        PlayerInventory cloneInv = clone.getInventory();
        int size = Math.min(originalInv.size(), cloneInv.size()); // not needed probably, but might as well be safe
        for (int i = 0; i < size; i++) {
            ItemStack stack = originalInv.getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound tag = stack.getNbt();
                if (tag != null && tag.contains(SOULBOUND_SLOT, NbtElement.NUMBER_TYPE)) {
                    cloneInv.setStack(i, stack);
                    // remove the slot tag, clear the tag if needed
                    tag.remove(SOULBOUND_SLOT);
                    if (tag.isEmpty()) {
                        stack.setNbt(null);
                    }
                }
            }
        }
    }
}
