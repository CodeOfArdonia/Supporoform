package slimeknights.tconstruct.smeltery.block.controller;

import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.multiblock.MultiblockResult;
import slimeknights.tconstruct.smeltery.network.StructureErrorPositionPacket;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Multiblock that displays the error from the tile entity on right click
 */
public abstract class HeatingControllerBlock extends ControllerBlock {
    protected HeatingControllerBlock(Settings builder) {
        super(builder);
    }

    @Override
    protected boolean openGui(PlayerEntity player, World world, BlockPos pos) {
        super.openGui(player, world, pos);
        // only need to update if holding the proper items
        if (!world.isClient) {
            BlockEntityHelper.get(HeatingStructureBlockEntity.class, world, pos).ifPresent(te -> {
                MultiblockResult result = te.getStructureResult();
                if (!result.isSuccess() && te.showDebugBlockBorder(player)) {
                    TinkerNetwork.getInstance().sendTo(new StructureErrorPositionPacket(pos, result.getPos()), player);
                }
            });
        }
        return true;
    }

    @Override
    protected boolean displayStatus(PlayerEntity player, World world, BlockPos pos, BlockState state) {
        if (!world.isClient) {
            BlockEntityHelper.get(HeatingStructureBlockEntity.class, world, pos).ifPresent(te -> {
                MultiblockResult result = te.getStructureResult();
                if (!result.isSuccess()) {
                    player.sendMessage(result.getMessage(), true);
                    TinkerNetwork.getInstance().sendTo(new StructureErrorPositionPacket(pos, result.getPos()), player);
                }
            });
        }
        return true;
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
