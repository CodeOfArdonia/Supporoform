package slimeknights.tconstruct.tools.modifiers.ability.armor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.dynamic.InventoryMenuModifier;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierLevelDisplay;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.nbt.*;

import static slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.isBlacklisted;

public class ToolBeltModifier extends InventoryMenuModifier implements VolatileDataModifierHook {
    private static final Pattern PATTERN = new Pattern(TConstruct.MOD_ID, "tool_belt");
    private static final Identifier SLOT_OVERRIDE = TConstruct.getResource("tool_belt_override");

    /**
     * Loader instance
     */
    public static final IGenericLoader<ToolBeltModifier> LOADER = new IGenericLoader<>() {
        @Override
        public ToolBeltModifier deserialize(JsonObject json) {
            JsonArray slotJson = JsonHelper.getArray(json, "level_slots");
            int[] slots = new int[slotJson.size()];
            // TODO: can this sort of thing be generalized?
            for (int i = 0; i < slots.length; i++) {
                slots[i] = JsonHelper.asInt(slotJson.get(i), "level_slots[" + i + "]");
                if (i > 0 && slots[i] <= slots[i - 1]) {
                    throw new JsonSyntaxException("level_slots must be increasing");
                }
            }
            return new ToolBeltModifier(slots);
        }

        @Override
        public ToolBeltModifier fromNetwork(PacketByteBuf buffer) {
            return new ToolBeltModifier(buffer.readIntArray());
        }

        @Override
        public void serialize(ToolBeltModifier object, JsonObject json) {
            JsonArray jsonArray = new JsonArray();
            for (int i : object.counts) {
                jsonArray.add(i);
            }
            json.add("level_slots", jsonArray);
        }

        @Override
        public void toNetwork(ToolBeltModifier object, PacketByteBuf buffer) {
            buffer.writeIntArray(object.counts);
        }
    };

    private final int[] counts;

    public ToolBeltModifier(int[] counts) {
        super(counts[0]);
        this.counts = counts;
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.VOLATILE_DATA);
    }

    @Override
    public IGenericLoader<? extends Modifier> getLoader() {
        return LOADER;
    }

    @Override
    public Text getDisplayName(int level) {
        return ModifierLevelDisplay.PLUSES.nameForLevel(this, level);
    }

    @Override
    public int getPriority() {
        return 85; // after shield strap, before pockets
    }

    /**
     * Gets the proper number of slots for the given level
     */
    private int getProperSlots(ModifierEntry entry) {
        int level = entry.intEffectiveLevel();
        if (level <= 0) {
            return 0;
        }
        if (level > this.counts.length) {
            return 9;
        } else {
            return this.counts[level - 1];
        }
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ModDataNBT volatileData) {
        int properSlots = this.getProperSlots(modifier);
        int slots;
        // find the largest slot index and either add or update the override as needed
        // TODO: can probably remove this code for 1.19
        if (properSlots < 9) {
            slots = properSlots;
            Identifier key = this.getInventoryKey();
            IModDataView modData = context.getPersistentData();
            if (modData.contains(key, NbtElement.LIST_TYPE)) {
                NbtList list = modData.get(key, GET_COMPOUND_LIST);
                int maxSlot = 0;
                for (int i = 0; i < list.size(); i++) {
                    int newSlot = list.getCompound(i).getInt(TAG_SLOT);
                    if (newSlot > maxSlot) {
                        maxSlot = newSlot;
                    }
                }
                maxSlot = Math.min(maxSlot + 1, 9);
                if (maxSlot > properSlots) {
                    volatileData.putInt(SLOT_OVERRIDE, maxSlot);
                    slots = maxSlot;
                }
            }
        } else {
            slots = 9;
        }
        ToolInventoryCapability.addSlots(volatileData, slots);
    }

    @Override
    public int getSlots(INamespacedNBTView volatileData, ModifierEntry modifier) {
        int properSlots = this.getProperSlots(modifier);
        if (properSlots >= 9) {
            return 9;
        }
        return MathHelper.clamp(volatileData.getInt(SLOT_OVERRIDE), properSlots, 9);
    }

    @Nullable
    @Override
    public Text validate(IToolStackView tool, ModifierEntry modifier) {
        return this.validateForMaxSlots(tool, this.getProperSlots(modifier));
    }

    @Override
    public boolean startInteract(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, EquipmentSlot equipmentSlot, TooltipKey keyModifier) {
        if (keyModifier == TooltipKey.SHIFT) {
            return super.startInteract(tool, modifier, player, equipmentSlot, keyModifier);
        }
        if (keyModifier == TooltipKey.NORMAL || keyModifier == TooltipKey.CONTROL) {
            if (player.getWorld().isClient) {
                return true;
            }

            boolean didChange = false;
            int slots = this.getSlots(tool, modifier);
            ModDataNBT persistentData = tool.getPersistentData();
            NbtList list = new NbtList();
            boolean[] swapped = new boolean[slots];
            // if we have existing items, swap stacks at each index
            PlayerInventory inventory = player.getInventory();
            Identifier key = this.getInventoryKey();
            if (persistentData.contains(key, NbtElement.LIST_TYPE)) {
                NbtList original = persistentData.get(key, GET_COMPOUND_LIST);
                if (!original.isEmpty()) {
                    for (int i = 0; i < original.size(); i++) {
                        NbtCompound compoundNBT = original.getCompound(i);
                        int slot = compoundNBT.getInt(TAG_SLOT);
                        if (slot < slots) {
                            // ensure we can store the hotbar item
                            ItemStack hotbar = inventory.getStack(slot);
                            if (hotbar.isEmpty() || !isBlacklisted(hotbar)) {
                                // swap the two items
                                ItemStack parsed = ItemStack.fromNbt(compoundNBT);
                                inventory.setStack(slot, parsed);
                                if (!hotbar.isEmpty()) {
                                    list.add(write(hotbar, slot));
                                }
                                didChange = true;
                            } else {
                                list.add(compoundNBT);
                            }
                            swapped[slot] = true;
                        }
                    }
                }
            }

            // list is empty, makes loop simplier
            for (int i = 0; i < slots; i++) {
                if (!swapped[i]) {
                    ItemStack hotbar = player.getInventory().getStack(i);
                    if (!hotbar.isEmpty() && !isBlacklisted(hotbar)) {
                        list.add(write(hotbar, i));
                        inventory.setStack(i, ItemStack.EMPTY);
                        didChange = true;
                    }
                }
            }

            // sound effect
            if (didChange) {
                persistentData.put(key, list);
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
        return hasStack ? null : PATTERN;
    }
}
