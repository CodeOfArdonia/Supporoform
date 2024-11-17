package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import net.minecraftforge.items.IItemHandler;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.inventory.EmptyItemHandler;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.mantle.util.WeakConsumerWrapper;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Shared logic between drains and ducts
 */
public abstract class SmelteryInputOutputBlockEntity<T> extends SmelteryComponentBlockEntity implements IRetexturedBlockEntity {
    /**
     * Capability this TE watches
     */
    private final Capability<T> capability;
    /**
     * Empty capability for in case the valid capability becomes invalid without invalidating
     */
    protected final T emptyInstance;
    /**
     * Listener to attach to consumed capabilities
     */
    protected final NonNullConsumer<LazyOptional<T>> listener = new WeakConsumerWrapper<>(this, (te, cap) -> te.clearHandler());
    @Nullable
    private LazyOptional<T> capabilityHolder = null;

    /* Retexturing */
    @Nonnull
    @Getter
    private Block texture = Blocks.AIR;

    protected SmelteryInputOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Capability<T> capability, T emptyInstance) {
        super(type, pos, state);
        this.capability = capability;
        this.emptyInstance = emptyInstance;
    }

    /**
     * Clears all cached capabilities
     */
    private void clearHandler() {
        if (this.capabilityHolder != null) {
            this.capabilityHolder.invalidate();
            this.capabilityHolder = null;
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.clearHandler();
    }

    @Override
    protected void setMaster(@Nullable BlockPos master, @Nullable Block block) {
        assert this.world != null;

        // if we have a new master, invalidate handlers
        boolean masterChanged = false;
        if (!Objects.equals(this.getMasterPos(), master)) {
            this.clearHandler();
            masterChanged = true;
        }
        super.setMaster(master, block);
        // notify neighbors of the change (state change skips the notify flag)
        if (masterChanged) {
            this.world.updateNeighbors(this.pos, this.getCachedState().getBlock());
        }
    }

    /**
     * Gets the capability to store in this IO block. Capability parent should have the proper listeners attached
     *
     * @param parent Parent tile entity
     * @return Capability from parent, or empty if absent
     */
    protected LazyOptional<T> getCapability(BlockEntity parent) {
        LazyOptional<T> handler = parent.getCapability(this.capability);
        if (handler.isPresent()) {
            handler.addListener(this.listener);

            return LazyOptional.of(() -> handler.orElse(this.emptyInstance));
        }
        return LazyOptional.empty();
    }

    /**
     * Fetches the capability handlers if missing
     */
    private LazyOptional<T> getCachedCapability() {
        if (this.capabilityHolder == null) {
            if (this.validateMaster()) {
                BlockPos master = this.getMasterPos();
                if (master != null && this.world != null) {
                    BlockEntity te = this.world.getBlockEntity(master);
                    if (te != null) {
                        this.capabilityHolder = this.getCapability(te);
                        return this.capabilityHolder;
                    }
                }
            }
            this.capabilityHolder = LazyOptional.empty();
        }
        return this.capabilityHolder;
    }

    @Nonnull
    @Override
    public <C> LazyOptional<C> getCapability(Capability<C> capability, @Nullable Direction facing) {
        if (capability == this.capability) {
            return this.getCachedCapability().cast();
        }
        return super.getCapability(capability, facing);
    }


    /* Retexturing */

    @Override
    @Nonnull
    public ModelData getModelData() {
        return RetexturedHelper.getModelData(this.getTexture());
    }

    @Override
    public String getTextureName() {
        return RetexturedHelper.getTextureName(this.texture);
    }

    @Override
    public void updateTexture(String name) {
        Block oldTexture = this.texture;
        this.texture = RetexturedHelper.getBlock(name);
        if (oldTexture != this.texture) {
            this.setChangedFast();
            RetexturedHelper.onTextureUpdated(this);
        }
    }


    /* NBT */

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    protected void saveSynced(NbtCompound tags) {
        super.saveSynced(tags);
        if (this.texture != Blocks.AIR) {
            tags.putString(TAG_TEXTURE, this.getTextureName());
        }
    }

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        if (tags.contains(TAG_TEXTURE, NbtElement.STRING_TYPE)) {
            this.texture = RetexturedHelper.getBlock(tags.getString(TAG_TEXTURE));
            RetexturedHelper.onTextureUpdated(this);
        }
    }


    /**
     * Fluid implementation of smeltery IO
     */
    public static abstract class SmelteryFluidIO extends SmelteryInputOutputBlockEntity<IFluidHandler> {
        protected SmelteryFluidIO(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, ForgeCapabilities.FLUID_HANDLER, EmptyFluidHandler.INSTANCE);
        }

        /**
         * Wraps the given capability
         */
        protected LazyOptional<IFluidHandler> makeWrapper(LazyOptional<IFluidHandler> capability) {
            return LazyOptional.of(() -> capability.orElse(this.emptyInstance));
        }

        @Override
        protected LazyOptional<IFluidHandler> getCapability(BlockEntity parent) {
            // fluid capability is not exposed directly in the smeltery
            if (parent instanceof ISmelteryTankHandler) {
                LazyOptional<IFluidHandler> capability = ((ISmelteryTankHandler) parent).getFluidCapability();
                if (capability.isPresent()) {
                    capability.addListener(this.listener);
                    return this.makeWrapper(capability);
                }
            }
            return LazyOptional.empty();
        }
    }

    /**
     * Item implementation of smeltery IO
     */
    public static class ChuteBlockEntity extends SmelteryInputOutputBlockEntity<IItemHandler> {
        public ChuteBlockEntity(BlockPos pos, BlockState state) {
            this(TinkerSmeltery.chute.get(), pos, state);
        }

        protected ChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state, ForgeCapabilities.ITEM_HANDLER, EmptyItemHandler.INSTANCE);
        }
    }

}
