package slimeknights.tconstruct.tables.block;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;

import org.jetbrains.annotations.Nullable;

@Getter
public class TinkerStationBlock extends RetexturedTableBlock {
    private final int slotCount;

    public TinkerStationBlock(Settings builder, int slotCount) {
        super(builder);
        this.slotCount = slotCount;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new TinkerStationBlockEntity(pPos, pState, this.getSlotCount());
    }
}
