package slimeknights.tconstruct.tables.block;

import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class CraftingStationBlock extends RetexturedTableBlock {

    public CraftingStationBlock(Settings builder) {
        super(builder);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new CraftingStationBlockEntity(pPos, pState);
    }
}
