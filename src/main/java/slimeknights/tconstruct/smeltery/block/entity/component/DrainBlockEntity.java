package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.ModelData;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.util.BlockEntityHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryInputOutputBlockEntity.SmelteryFluidIO;
import slimeknights.tconstruct.smeltery.block.entity.tank.IDisplayFluidListener;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Fluid IO extension to display controller fluid
 */
public class DrainBlockEntity extends SmelteryFluidIO implements IDisplayFluidListener {
    @Getter
    private FluidStack displayFluid = FluidStack.EMPTY;

    public DrainBlockEntity(BlockPos pos, BlockState state) {
        this(TinkerSmeltery.drain.get(), pos, state);
    }

    protected DrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        return RetexturedHelper.getModelDataBuilder(getTexture()).with(IDisplayFluidListener.PROPERTY, displayFluid).build();
    }

    @Override
    public void notifyDisplayFluidUpdated(FluidStack fluid) {
        if (!fluid.isFluidEqual(displayFluid)) {
            // no need to copy as the fluid was copied by the caller
            displayFluid = fluid;
            requestModelDataUpdate();
            assert world != null;
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, 48);
        }
    }

    @Override
    public BlockPos getListenerPos() {
        return getPos();
    }


    /* Updating */

    /**
     * Attaches this TE to the master as a display fluid listener
     */
    private void attachFluidListener() {
        BlockPos masterPos = getMasterPos();
        if (masterPos != null && world != null && world.isClient) {
            BlockEntityHelper.get(ISmelteryTankHandler.class, world, masterPos).ifPresent(te -> te.addDisplayListener(this));
        }
    }

    // override instead of writeSynced to avoid writing master to the main tag twice
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = super.toInitialChunkDataNbt();
        writeMaster(nbt);
        return nbt;
    }

    @Override
    public void handleUpdateTag(NbtCompound tag) {
        BlockPos oldMaster = getMasterPos();
        super.handleUpdateTag(tag);
        if (!Objects.equals(oldMaster, getMasterPos())) {
            attachFluidListener();
        }
    }

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    public void onDataPacket(ClientConnection net, BlockEntityUpdateS2CPacket pkt) {
        NbtCompound tag = pkt.getNbt();
        if (tag != null) {
            BlockPos oldMaster = getMasterPos();
            readNbt(tag);
            if (!Objects.equals(oldMaster, getMasterPos())) {
                attachFluidListener();
            }

        }
    }
}
