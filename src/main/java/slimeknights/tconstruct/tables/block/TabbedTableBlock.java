package slimeknights.tconstruct.tables.block;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import slimeknights.tconstruct.shared.block.TableBlock;

public abstract class TabbedTableBlock extends TableBlock implements ITabbedBlock {

    public TabbedTableBlock(Settings builder) {
        super(builder);
    }

    @Override
    public boolean openGui(PlayerEntity player, World world, BlockPos pos) {
        return super.openGui(player, world, pos);
    }
}
