package slimeknights.tconstruct.tools.item;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;

public class ModifiableSwordItem extends ModifiableItem {
    public ModifiableSwordItem(Settings properties, ToolDefinition toolDefinition) {
        super(properties, toolDefinition);
    }

    @Override
    public boolean canMine(BlockState state, World worldIn, BlockPos pos, PlayerEntity player) {
        return !player.isCreative();
    }
}
