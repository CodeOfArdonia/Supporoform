package slimeknights.tconstruct.tables.block;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType.BlockEntityFactory;
import net.minecraft.util.math.BlockPos;

/**
 * Generic block shared by any that don't need special stuff on top
 */
public class GenericTableBlock extends RetexturedTableBlock {
    private final BlockEntityFactory<? extends BlockEntity> blockEntity;

    public GenericTableBlock(Settings builder, BlockEntityFactory<? extends BlockEntity> blockEntity) {
        super(builder);
        this.blockEntity = blockEntity;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return this.blockEntity.create(pos, state);
    }
}
