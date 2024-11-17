package slimeknights.tconstruct.tables.block.entity.chest;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.ScalingChestItemHandler;

/**
 * Chest that holds parts, up to 8 of a given material and type
 */
public class PartChestBlockEntity extends AbstractChestBlockEntity {
    private static final Text NAME = TConstruct.makeTranslation("gui", "part_chest");

    public PartChestBlockEntity(BlockPos pos, BlockState state) {
        super(TinkerTables.partChestTile.get(), pos, state, NAME, new PartChestItemHandler());
    }

    /**
     * Item handler for part chests
     */
    public static class PartChestItemHandler extends ScalingChestItemHandler {
        @Override
        public int getSlotLimit(int slot) {
            return 8;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // check if there is no other slot containing that item
            for (int i = 0; i < this.getSlots().size(); i++) {
                // don't compare count
                if (ItemStack.areItemsEqual(stack, this.getStackInSlot(i)) && ItemStack.tagMatches(stack, this.getStackInSlot(i))) {
                    return i == slot; // only allowed in the same slot
                }
            }
            // TODO 1.20: ditch material item check in favor of the tag
            return stack.isIn(TinkerTags.Items.CHEST_PARTS) || stack.getItem() instanceof IMaterialItem;
        }
    }
}
