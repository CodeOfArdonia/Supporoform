package slimeknights.mantle.block;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.ItemUsageContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Log block that can be stripped
 */
public class StrippableLogBlock extends PillarBlock {
    private final Supplier<? extends Block> stripped;

    public StrippableLogBlock(Supplier<? extends Block> stripped, Settings properties) {
        super(properties);
        this.stripped = stripped;
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, ItemUsageContext context, ToolAction toolAction, boolean simulate) {
        if (toolAction == ToolActions.AXE_STRIP) {
            return this.stripped.get().getDefaultState().with(AXIS, state.get(AXIS));
        }
        return null;
    }
}
