package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType.BlockEntityFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

public class RetexturedOrientableSmelteryBlock extends OrientableSmelteryBlock {
    public RetexturedOrientableSmelteryBlock(Settings properties, BlockEntityFactory<? extends SmelteryComponentBlockEntity> blockEntity) {
        super(properties, true, blockEntity);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        RetexturedBlock.updateTextureBlock(world, pos, stack);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return RetexturedBlock.getPickBlock(world, pos, state);
    }
}
