package slimeknights.tconstruct.tables.block.entity.chest;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.ScalingChestItemHandler;

/**
 * Chest that holds casts, up to 4 of every type
 */
public class CastChestBlockEntity extends AbstractChestBlockEntity {
    private static final Text NAME = TConstruct.makeTranslation("gui", "cast_chest");

    public CastChestBlockEntity(BlockPos pos, BlockState state) {
        super(TinkerTables.castChestTile.get(), pos, state, NAME, new CastChestIItemHandler());
    }

    /**
     * Item handler for cast chests
     */
    public static class CastChestIItemHandler extends ScalingChestItemHandler {
        @Override
        public int getSlotLimit(int slot) {
            return 4;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            for (int i = 0; i < this.getSlots().size(); i++) {
                if (ItemStack.areItemsEqual(stack, this.getStackInSlot(i))) {
                    return i == slot;
                }
            }
            return stack.isIn(TinkerTags.Items.CASTS);
        }
    }
}
