package slimeknights.tconstruct.tables.client.inventory.module;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import slimeknights.mantle.client.screen.MultiModuleScreen;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.mantle.inventory.EmptyItemHandler;
import slimeknights.tconstruct.tables.block.entity.inventory.IScalingContainer;

import java.util.Optional;

public class ScalingChestScreen<T extends BlockEntity> extends DynamicContainerScreen {
    private final IScalingContainer scaling;

    public ScalingChestScreen(MultiModuleScreen<?> parent, BaseContainerMenu<T> container, PlayerInventory playerInventory, Text title) {
        super(parent, container, playerInventory, title);
        BlockEntity tile = container.getTile();
        IItemHandler handler = Optional.ofNullable(tile)
                .flatMap(t -> t.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve())
                .orElse(EmptyItemHandler.INSTANCE);
        this.scaling = handler instanceof IScalingContainer ? (IScalingContainer) handler : handler::getSlots;
        this.slotCount = this.scaling.getVisualSize();
        this.sliderActive = true;
    }

    @Override
    public void updatePosition(int parentX, int parentY, int parentSizeX, int parentSizeY) {
        this.x = parentX + this.xOffset;
        this.y = parentY + this.yOffset;

        // calculate rows and columns from space
        this.columns = (this.backgroundWidth - this.slider.width) / slot.w;
        this.rows = this.backgroundHeight / slot.h;

        this.updateSlider();
        this.updateSlots();
    }

    @Override
    protected void updateSlider() {
        this.sliderActive = this.slotCount > this.columns * this.rows;
        super.updateSlider();
        this.slider.setEnabled(this.sliderActive);
        this.slider.show();
    }

    @Override
    public void update(int mouseX, int mouseY) {
        this.slotCount = this.scaling.getVisualSize();
        super.update(mouseX, mouseY);
        this.updateSlider();
        this.updateSlots();
    }

    @Override
    public boolean shouldDrawSlot(Slot slot) {
        if (slot.getSlotIndex() >= this.scaling.getVisualSize()) {
            return false;
        }
        return super.shouldDrawSlot(slot);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }
}
