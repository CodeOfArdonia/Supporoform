package slimeknights.tconstruct.tables.block.entity.inventory;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.library.recipe.ITinkerableContainer;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tables.block.entity.table.ModifierWorktableBlockEntity;

import org.jetbrains.annotations.Nullable;

import static slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.TINKER_SLOT;

@RequiredArgsConstructor
public class ModifierWorktableContainerWrapper implements ITinkerableContainer.Mutable {
    private final ModifierWorktableBlockEntity worktable;
    @Nullable
    @Setter
    private PlayerEntity player;

    /**
     * Cached tool instance to save lookup effort
     */
    @Nullable
    private ToolStack tool;

    /**
     * Clears the cached inputs
     */
    public void refreshInput(int slot) {
        if (slot == TINKER_SLOT) {
            tool = null;
        }
    }

    @Override
    public ItemStack getTinkerableStack() {
        return worktable.getStack(ModifierWorktableBlockEntity.TINKER_SLOT);
    }

    @Override
    public ToolStack getTinkerable() {
        if (tool == null) {
            tool = ToolStack.from(getTinkerableStack());
        }
        return this.tool;
    }

    @Override
    public ItemStack getInput(int index) {
        return this.worktable.getStack(index + ModifierWorktableBlockEntity.INPUT_START);
    }

    @Override
    public int getInputCount() {
        return ModifierWorktableBlockEntity.INPUT_COUNT;
    }

    @Override
    public void setInput(int index, ItemStack stack) {
        if (index >= 0 && index < ModifierWorktableBlockEntity.INPUT_COUNT) {
            this.worktable.setStack(index + ModifierWorktableBlockEntity.INPUT_START, stack);
        }
    }

    @Override
    public void giveItem(ItemStack stack) {
        if (this.player != null) {
            this.player.getInventory().offerOrDrop(stack);
        }
    }
}
