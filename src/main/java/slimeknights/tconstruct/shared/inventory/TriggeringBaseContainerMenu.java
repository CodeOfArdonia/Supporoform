package slimeknights.tconstruct.shared.inventory;

import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.tconstruct.shared.TinkerCommons;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandlerType;

/**
 * Container that triggers the criteria instance
 */
public class TriggeringBaseContainerMenu<TILE extends BlockEntity> extends BaseContainerMenu<TILE> {
    protected TriggeringBaseContainerMenu(ScreenHandlerType<?> type, int id, @Nullable PlayerInventory inv, @Nullable TILE tile) {
        super(type, id, inv, tile);
        TinkerCommons.CONTAINER_OPENED_TRIGGER.trigger(tile, inv);
    }
}
