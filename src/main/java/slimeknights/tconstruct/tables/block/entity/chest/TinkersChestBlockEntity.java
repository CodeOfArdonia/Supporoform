package slimeknights.tconstruct.tables.block.entity.chest;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;

/**
 * Chest holding 64 slots of 16 items each
 */
public class TinkersChestBlockEntity extends AbstractChestBlockEntity {
    /**
     * NBT tag for colors of the chest
     */
    public static final String TAG_CHEST_COLOR = "color";
    /**
     * Default color for a chest
     */
    public static final int DEFAULT_COLOR = 0x407686;
    public static final Text NAME = TConstruct.makeTranslation("gui", "tinkers_chest");

    /**
     * Current display color for the chest
     */
    @Getter
    private int color = DEFAULT_COLOR;
    /**
     * If true, a custom color was set
     */
    @Getter
    @Accessors(fluent = true)
    private boolean hasColor = false;

    public TinkersChestBlockEntity(BlockPos pos, BlockState state) {
        super(TinkerTables.tinkersChestTile.get(), pos, state, NAME, new TinkersChestItemHandler());
    }

    /**
     * Sets the color of the chest
     */
    public void setColor(int color) {
        this.color = color;
        this.hasColor = true;
    }

    @Override
    public boolean canInsert(PlayerEntity player, ItemStack heldItem) {
        return false;
    }

    @Override
    public void saveSynced(NbtCompound tags) {
        super.saveSynced(tags);
        if (this.hasColor) {
            tags.putInt(TAG_CHEST_COLOR, this.color);
        }
    }

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        if (tags.contains(TAG_CHEST_COLOR, NbtElement.NUMBER_TYPE)) {
            this.setColor(tags.getInt(TAG_CHEST_COLOR));
        }
    }

    /**
     * Item handler for tinkers chests
     */
    public static class TinkersChestItemHandler extends ItemStackHandler implements IChestItemHandler {
        @Setter
        @Nullable
        private MantleBlockEntity parent;

        public TinkersChestItemHandler() {
            super(64);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 16;
        }

        @Override
        public int getVisualSize() {
            return getSlots().size();
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (this.parent != null) {
                this.parent.setChangedFast();
            }
        }
    }
}
