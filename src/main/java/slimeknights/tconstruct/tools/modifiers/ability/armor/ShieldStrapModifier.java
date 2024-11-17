package slimeknights.tconstruct.tools.modifiers.ability.armor;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.dynamic.InventoryMenuModifier;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

public class ShieldStrapModifier extends InventoryMenuModifier {
    private static final Identifier KEY = TConstruct.getResource("shield_strap");
    private static final Pattern PATTERN = new Pattern(TConstruct.MOD_ID, "shield_plus");

    public ShieldStrapModifier() {
        super(KEY, 1);
    }

    @Override
    public int getPriority() {
        return 95; // before pockets and tool belt
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ModDataNBT volatileData) {
        super.addVolatileData(context, modifier, volatileData);
        volatileData.putBoolean(ToolInventoryCapability.INCLUDE_OFFHAND, true);
    }

    @Override
    public boolean startInteract(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, EquipmentSlot equipmentSlot, TooltipKey keyModifier) {
        if (keyModifier == TooltipKey.SHIFT) {
            return super.startInteract(tool, modifier, player, equipmentSlot, keyModifier);
        }
        if (keyModifier == TooltipKey.NORMAL) {
            if (player.getWorld().isClient) {
                return true;
            }
            // offhand must be able to go in the pants
            ItemStack offhand = player.getOffHandStack();
            int slots = this.getSlots(tool, modifier);
            if (offhand.isEmpty() || !ToolInventoryCapability.isBlacklisted(offhand)) {
                ItemStack newOffhand = ItemStack.EMPTY;
                ModDataNBT persistentData = tool.getPersistentData();
                NbtList list = new NbtList();
                // if we have existing items, shift all back by 1
                if (persistentData.contains(KEY, NbtElement.LIST_TYPE)) {
                    NbtList original = persistentData.get(KEY, GET_COMPOUND_LIST);
                    for (int i = 0; i < original.size(); i++) {
                        NbtCompound compoundNBT = original.getCompound(i);
                        int slot = compoundNBT.getInt(TAG_SLOT);
                        if (slot == 0) {
                            newOffhand = ItemStack.fromNbt(compoundNBT);
                        } else if (slot < slots) {
                            NbtCompound copy = compoundNBT.copy();
                            copy.putInt(TAG_SLOT, slot - 1);
                            list.add(copy);
                        }
                    }
                }
                // add old offhand to the list
                if (!offhand.isEmpty()) {
                    list.add(write(offhand, slots - 1));
                }
                // update offhand
                persistentData.put(KEY, list);
                player.setStackInHand(Hand.OFF_HAND, newOffhand);

                // sound effect
                if (!newOffhand.isEmpty() || !list.isEmpty()) {
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
        return hasStack ? null : PATTERN;
    }
}
