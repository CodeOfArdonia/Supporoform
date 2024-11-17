package slimeknights.tconstruct.smeltery.menu;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.tconstruct.shared.inventory.TriggeringBaseContainerMenu;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import org.jetbrains.annotations.Nullable;

/**
 * Container for a block with a single item inventory
 */
public class SingleItemContainerMenu extends TriggeringBaseContainerMenu<BlockEntity> {
    public SingleItemContainerMenu(int id, @Nullable PlayerInventory inv, @Nullable BlockEntity te) {
        super(TinkerSmeltery.singleItemContainer.get(), id, inv, te);
        if (te != null) {
            te.getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .ifPresent(handler -> this.addSlot(new SmartItemHandlerSlot(handler, 0, 80, 20)));
            this.addInventorySlots();
        }
    }

    public SingleItemContainerMenu(int id, PlayerInventory inv, PacketByteBuf buf) {
        this(id, inv, getTileEntityFromBuf(buf, BlockEntity.class));
    }

    @Override
    protected int getInventoryYOffset() {
        return 51;
    }
}
