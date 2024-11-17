package slimeknights.tconstruct.tables.menu.slot;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

/**
 * Slot for accessing player armor
 */
public class ArmorSlot extends Slot {
    private static final Identifier[] ARMOR_SLOT_BACKGROUNDS = new Identifier[]{
            PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE
    };

    private final PlayerEntity player;
    private final EquipmentSlot slotType;

    public ArmorSlot(PlayerInventory inv, EquipmentSlot slotType, int xPosition, int yPosition) {
        super(inv, 36 + slotType.getEntitySlotId(), xPosition, yPosition);
        this.player = inv.player;
        this.slotType = slotType;
        setBackground(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, ARMOR_SLOT_BACKGROUNDS[slotType.getEntitySlotId()]);
    }

    @Override
    public int getMaxItemCount() {
        return 1;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return stack.canEquip(this.slotType, this.player);
    }

    @Override
    public boolean canTakeItems(PlayerEntity player) {
        ItemStack stack = this.getStack();
        return stack.isEmpty() || player.isCreative() || !EnchantmentHelper.hasBindingCurse(stack);
    }
}
