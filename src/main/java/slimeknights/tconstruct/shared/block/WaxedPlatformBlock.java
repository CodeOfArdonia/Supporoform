package slimeknights.tconstruct.shared.block;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable.OxidationLevel;
import net.minecraft.item.ItemUsageContext;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.TinkerCommons;

public class WaxedPlatformBlock extends PlatformBlock {
    private final OxidationLevel age;

    public WaxedPlatformBlock(OxidationLevel age, Settings prop) {
        super(prop);
        this.age = age;
    }

    @Override
    protected boolean verticalConnect(BlockState state) {
        return state.isIn(TinkerTags.Blocks.COPPER_PLATFORMS);
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, ItemUsageContext context, ToolAction toolAction, boolean simulate) {
        if (ToolActions.AXE_WAX_OFF.equals(toolAction)) {
            return TinkerCommons.copperPlatform.get(this.age).getStateWithProperties(state);
        }
        return null;
    }
}
