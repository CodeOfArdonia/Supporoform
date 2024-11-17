package slimeknights.tconstruct.library.tools.helper;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * Generic modifier hooks that don't quite fit elsewhere
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModifierUtil {
    /**
     * Drops an item at the entity position
     */
    public static void dropItem(Entity target, ItemStack stack) {
        if (!stack.isEmpty() && !target.getWorld().isClient) {
            ItemEntity ent = new ItemEntity(target.getWorld(), target.getX(), target.getY() + 1, target.getZ(), stack);
            ent.setToDefaultPickupDelay();
            Random rand = target.getWorld().random;
            ent.setVelocity(ent.getVelocity().add((rand.nextFloat() - rand.nextFloat()) * 0.1F,
                    rand.nextFloat() * 0.05F,
                    (rand.nextFloat() - rand.nextFloat()) * 0.1F));
            target.getWorld().spawnEntity(ent);
        }
    }

    /**
     * Gets the entity as a living entity, or null if they are not a living entity
     */
    @Nullable
    public static LivingEntity asLiving(@Nullable Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    /**
     * Gets the entity as a player, or null if they are not a player
     */
    @Nullable
    public static PlayerEntity asPlayer(@Nullable Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return player;
        }
        return null;
    }

    /**
     * Direct method to get the level of a modifier from a stack. If you need to get multiple modifier levels, using {@link ToolStack} is faster
     *
     * @param stack    Stack to check
     * @param modifier Modifier to search for
     * @return Modifier level, or 0 if not present or the stack is not modifiable
     */
    public static int getModifierLevel(ItemStack stack, ModifierId modifier) {
        if (!stack.isEmpty() && stack.isIn(TinkerTags.Items.MODIFIABLE)) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null && nbt.contains(ToolStack.TAG_MODIFIERS, NbtElement.LIST_TYPE)) {
                NbtList list = nbt.getList(ToolStack.TAG_MODIFIERS, NbtElement.COMPOUND_TYPE);
                int size = list.size();
                if (size > 0) {
                    String key = modifier.toString();
                    for (int i = 0; i < size; i++) {
                        NbtCompound entry = list.getCompound(i);
                        if (key.equals(entry.getString(ModifierEntry.TAG_MODIFIER))) {
                            return entry.getInt(ModifierEntry.TAG_LEVEL);
                        }
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Checks if the given stack has upgrades
     */
    public static boolean hasUpgrades(ItemStack stack) {
        if (!stack.isEmpty() && stack.isIn(TinkerTags.Items.MODIFIABLE)) {
            NbtCompound nbt = stack.getNbt();
            return nbt != null && !nbt.getList(ToolStack.TAG_UPGRADES, NbtElement.COMPOUND_TYPE).isEmpty();
        }
        return false;
    }

    /**
     * Checks if the given slot may contain armor
     */
    public static boolean validArmorSlot(LivingEntity living, EquipmentSlot slot) {
        return slot.getType() == Type.ARMOR || living.getEquippedStack(slot).isIn(TinkerTags.Items.HELD);
    }

    /**
     * Checks if the given slot may contain armor
     */
    public static boolean validArmorSlot(IToolStackView tool, EquipmentSlot slot) {
        return slot.getType() == Type.ARMOR || tool.hasTag(TinkerTags.Items.HELD);
    }

    /**
     * Shortcut to get a volatile flag when the tool stack is not needed otherwise
     */
    public static boolean checkVolatileFlag(ItemStack stack, Identifier flag) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(ToolStack.TAG_VOLATILE_MOD_DATA, NbtElement.COMPOUND_TYPE)) {
            return nbt.getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).getBoolean(flag.toString());
        }
        return false;
    }

    /**
     * Shortcut to get a volatile int value when the tool stack is not needed otherwise
     */
    public static int getVolatileInt(ItemStack stack, Identifier flag) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(ToolStack.TAG_VOLATILE_MOD_DATA, NbtElement.COMPOUND_TYPE)) {
            return nbt.getCompound(ToolStack.TAG_VOLATILE_MOD_DATA).getInt(flag.toString());
        }
        return 0;
    }

    /**
     * Shortcut to get a volatile int value when the tool stack is not needed otherwise
     */
    public static int getPersistentInt(ItemStack stack, Identifier flag, int defealtValue) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(ToolStack.TAG_PERSISTENT_MOD_DATA, NbtElement.COMPOUND_TYPE)) {
            NbtCompound persistent = nbt.getCompound(ToolStack.TAG_PERSISTENT_MOD_DATA);
            String flagString = flag.toString();
            if (persistent.contains(flagString, NbtElement.INT_TYPE)) {
                return persistent.getInt(flagString);
            }
        }
        return defealtValue;
    }

    /**
     * Shortcut to get a persistent string value when the tool stack is not needed otherwise
     */
    public static String getPersistentString(ItemStack stack, Identifier flag) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(ToolStack.TAG_PERSISTENT_MOD_DATA, NbtElement.COMPOUND_TYPE)) {
            return nbt.getCompound(ToolStack.TAG_PERSISTENT_MOD_DATA).getString(flag.toString());
        }
        return "";
    }

    /**
     * Checks if a tool can perform the given action
     */
    public static boolean canPerformAction(IToolStackView tool, ToolAction action) {
        if (!tool.isBroken()) {
            // can the tool do this action inherently?
            if (tool.getHook(ToolHooks.TOOL_ACTION).canPerformAction(tool, action)) {
                return true;
            }
            for (ModifierEntry entry : tool.getModifierList()) {
                if (entry.getHook(ModifierHooks.TOOL_ACTION).canPerformAction(tool, entry, action)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calculates inaccuracy from the conditional tool stat.
     */
    public static float getInaccuracy(IToolStackView tool, LivingEntity living) {
        return 3 * (1 / ConditionalStatModifierHook.getModifiedStat(tool, living, ToolStats.ACCURACY) - 1);
    }

    /**
     * Interface used for {@link #foodConsumer}
     */
    public interface FoodConsumer {
        /**
         * Called when food is eaten to notify compat that food was eaten
         */
        void onConsume(PlayerEntity player, ItemStack stack, int hunger, float saturation);
    }

    /**
     * Instance of the current food consumer, will be either no-op or an implementation calling the Diet API, never null.
     */
    @NotNull
    public static FoodConsumer foodConsumer = (player, stack, hunger, saturation) -> {
    };
}
