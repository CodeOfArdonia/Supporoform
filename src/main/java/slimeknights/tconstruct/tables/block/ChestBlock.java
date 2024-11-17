package slimeknights.tconstruct.tables.block;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType.BlockEntityFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.tconstruct.tables.block.entity.chest.AbstractChestBlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * Shared block logic for all chest types
 */
public class ChestBlock extends TabbedTableBlock {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D), //top
            Block.createCuboidShape(1.0D, 3.0D, 1.0D, 15.0D, 16.0D, 15.0D), //middle
            Block.createCuboidShape(0.5D, 0.0D, 0.5D, 2.5D, 15.0D, 2.5D), //leg
            Block.createCuboidShape(13.5D, 0.0D, 0.5D, 15.5D, 15.0D, 2.5D), //leg
            Block.createCuboidShape(13.5D, 0.0D, 13.5D, 15.5D, 15.0D, 15.5D), //leg
            Block.createCuboidShape(0.5D, 0.0D, 13.5D, 2.5D, 15.0D, 15.5D) //leg
    );

    private final BlockEntityFactory<? extends BlockEntity> blockEntity;
    private final boolean dropsItems;

    public ChestBlock(Settings builder, BlockEntityFactory<? extends BlockEntity> blockEntity, boolean dropsItems) {
        super(builder);
        this.blockEntity = blockEntity;
        this.dropsItems = dropsItems;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return this.blockEntity.create(pPos, pState);
    }

    @Override
    public void onPlaced(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(worldIn, pos, state, placer, stack);
        // check if we also have an inventory

        NbtCompound tag = stack.getNbt();
        if (tag != null && tag.contains("TinkerData", NbtElement.COMPOUND_TYPE)) {
            NbtCompound tinkerData = tag.getCompound("TinkerData");
            BlockEntity te = worldIn.getBlockEntity(pos);
            if (te instanceof AbstractChestBlockEntity chest) {
                chest.readInventory(tinkerData);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockHitResult hit) {
        BlockEntity te = worldIn.getBlockEntity(pos);
        PlayerInventory playerInventory = player.getInventory();
        ItemStack heldItem = playerInventory.getMainHandStack();

        if (!heldItem.isEmpty() && te instanceof AbstractChestBlockEntity chest && chest.canInsert(player, heldItem)) {
            IItemHandlerModifiable itemHandler = chest.getItemHandler();
            ItemStack rest = ItemHandlerHelper.insertItem(itemHandler, heldItem, false);
            if (rest.isEmpty() || rest.getCount() < heldItem.getCount()) {
                playerInventory.main.set(playerInventory.selectedSlot, rest);
                return ActionResult.SUCCESS;
            }
        }

        return super.onUse(state, worldIn, pos, player, handIn, hit);
    }

    @Override
    protected void dropInventoryItems(BlockState state, World worldIn, BlockPos pos, IItemHandler inventory) {
        if (this.dropsItems) {
            dropInventoryItems(worldIn, pos, inventory);
        }
    }
}
