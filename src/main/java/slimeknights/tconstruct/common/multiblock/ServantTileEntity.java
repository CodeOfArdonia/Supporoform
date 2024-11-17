package slimeknights.tconstruct.common.multiblock;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.tconstruct.library.utils.TagUtil;

import org.jetbrains.annotations.Nullable;

public class ServantTileEntity extends MantleBlockEntity implements IServantLogic {
    private static final String TAG_MASTER_POS = "masterOffset";
    private static final String TAG_MASTER_BLOCK = "masterBlock";

    @Getter
    @Nullable
    private BlockPos masterPos;
    @Nullable
    private Block masterBlock;

    public ServantTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Checks if this servant has a master
     */
    public boolean hasMaster() {
        return this.masterPos != null;
    }

    /**
     * Called to change the master
     *
     * @param master New master
     * @param block  New master block
     */
    protected void setMaster(@Nullable BlockPos master, @Nullable Block block) {
        this.masterPos = master;
        this.masterBlock = block;
        this.setChangedFast();
    }

    /**
     * Checks that this servant has a valid master. Clears the master if invalid
     *
     * @return True if this servant has a valid master
     */
    protected boolean validateMaster() {
        if (this.masterPos == null) {
            return false;
        }

        // ensure the master block is correct
        assert this.world != null;
        if (this.world.getBlockState(this.masterPos).getBlock() == this.masterBlock) {
            return true;
        }
        // master invalid, so clear
        this.setMaster(null, null);
        return false;
    }

    @Override
    public boolean isValidMaster(IMasterLogic master) {
        // if we have a valid master, the passed master is only valid if its our current master
        if (this.validateMaster()) {
            return master.getMasterPos().equals(this.masterPos);
        }
        // otherwise, we are happy with any master
        return true;
    }

    @Override
    public void notifyMasterOfChange(BlockPos pos, BlockState state) {
        if (this.validateMaster()) {
            assert this.masterPos != null;
            BlockEntityHelper.get(IMasterLogic.class, this.world, this.masterPos).ifPresent(te -> te.notifyChange(pos, state));
        }
    }

    @Override
    public void setPotentialMaster(IMasterLogic master) {
        BlockPos newMaster = master.getMasterPos();
        // if this is our current master, simply update the master block
        if (newMaster.equals(this.masterPos)) {
            this.masterBlock = master.getMasterBlock().getBlock();
            this.setChangedFast();
            // otherwise, only set if we don't have a master
        } else if (!this.validateMaster()) {
            this.setMaster(newMaster, master.getMasterBlock().getBlock());
        }
    }

    @Override
    public void removeMaster(IMasterLogic master) {
        if (this.masterPos != null && this.masterPos.equals(master.getMasterPos())) {
            this.setMaster(null, null);
        }
    }


    /* NBT */

    /**
     * Reads the master from NBT
     *
     * @param tags NBT to read
     */
    protected void readMaster(NbtCompound tags) {
        BlockPos masterPos = TagUtil.readOptionalPos(tags, TAG_MASTER_POS, this.pos);
        Block masterBlock = null;
        // if the master position is valid, get the master block
        if (masterPos != null && tags.contains(TAG_MASTER_BLOCK, NbtElement.STRING_TYPE)) {
            Identifier masterBlockName = Identifier.tryParse(tags.getString(TAG_MASTER_BLOCK));
            if (masterBlockName != null && Registries.BLOCK.containsId(masterBlockName)) {
                masterBlock = Registries.BLOCK.get(masterBlockName);
            }
        }
        // if both valid, set
        if (masterBlock != null) {
            this.masterPos = masterPos;
            this.masterBlock = masterBlock;
        }
    }

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        this.readMaster(tags);
    }

    /**
     * Writes the master position and master block to the given compound
     *
     * @param tags Tags
     */
    protected NbtCompound writeMaster(NbtCompound tags) {
        if (this.masterPos != null && this.masterBlock != null) {
            tags.put(TAG_MASTER_POS, NbtHelper.fromBlockPos(this.masterPos.subtract(this.pos)));
            tags.putString(TAG_MASTER_BLOCK, Registries.BLOCK.getKey(this.masterBlock).toString());
        }
        return tags;
    }

    @Override
    public void writeNbt(NbtCompound tags) {
        super.writeNbt(tags);
        this.writeMaster(tags);
    }
}
