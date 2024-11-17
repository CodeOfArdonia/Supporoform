package slimeknights.mantle.inventory;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Used to wrap the slots inside Modules/Subcontainers
 */
public class WrapperSlot extends Slot {

    public final Slot parent;

    public WrapperSlot(Slot slot) {
        super(slot.inventory, slot.getSlotIndex(), slot.x, slot.y);
        this.parent = slot;
    }

    @Override
    public void onQuickTransfer(ItemStack oldStack, ItemStack newStack) {
        this.parent.onQuickTransfer(oldStack, newStack);
    }

    @Override
    public void onTakeItem(PlayerEntity playerIn, ItemStack stack) {
        this.parent.onTakeItem(playerIn, stack);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return this.parent.canInsert(stack);
    }

    @Override
    public ItemStack getStack() {
        return this.parent.getStack();
    }

    @Override
    public boolean hasStack() {
        return this.parent.hasStack();
    }

    @Override
    public void setStack(ItemStack stack) {
        this.parent.setStack(stack);
    }

    @Override
    public void setStackNoCallbacks(ItemStack stack) {
        this.parent.setStackNoCallbacks(stack);
    }

    @Override
    public void markDirty() {
        this.parent.markDirty();
    }

    @Override
    public int getMaxItemCount() {
        return this.parent.getMaxItemCount();
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        return this.parent.getMaxItemCount(stack);
    }

    @Override
    public Pair<Identifier, Identifier> getBackgroundSprite() {
        return this.parent.getBackgroundSprite();
    }

    @Override
    public ItemStack takeStack(int amount) {
        return this.parent.takeStack(amount);
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerIn) {
        return this.parent.canTakeItems(playerIn);
    }

    @Override
    public boolean isEnabled() {
        return this.parent.isEnabled();
    }

    @Override
    public Slot setBackground(Identifier atlas, Identifier sprite) {
        return this.parent.setBackground(atlas, sprite);
    }

    @Override
    public Optional<ItemStack> tryTakeStackRange(int pCount, int pDecrement, PlayerEntity pPlayer) {
        return this.parent.tryTakeStackRange(pCount, pDecrement, pPlayer);
    }

    @Override
    public ItemStack takeStackRange(int pCount, int pDecrement, PlayerEntity pPlayer) {
        return this.parent.takeStackRange(pCount, pDecrement, pPlayer);
    }

    @Override
    public ItemStack insertStack(ItemStack pStack, int pIncrement) {
        return this.parent.insertStack(pStack, pIncrement);
    }

    @Override
    public boolean canTakePartial(PlayerEntity pPlayer) {
        return this.parent.canTakePartial(pPlayer);
    }

    @Override
    public boolean canBeHighlighted() {
        return this.parent.canBeHighlighted();
    }
}
