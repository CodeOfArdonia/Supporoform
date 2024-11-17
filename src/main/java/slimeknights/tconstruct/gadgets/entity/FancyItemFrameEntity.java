package slimeknights.tconstruct.gadgets.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.utils.Util;

public class FancyItemFrameEntity extends ItemFrameEntity {
    private static final int DIAMOND_TIMER = 300;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(FancyItemFrameEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final String TAG_VARIANT = "Variant";
    private static final String TAG_ROTATION_TIMER = "RotationTimer";

    private int rotationTimer = 0;

    public FancyItemFrameEntity(EntityType<? extends FancyItemFrameEntity> type, World level) {
        super(type, level);
    }

    public FancyItemFrameEntity(World levelIn, BlockPos blockPos, Direction face, FrameType variant) {
        super(TinkerGadgets.itemFrameEntity.get(), levelIn);
        this.attachmentPos = blockPos;
        this.setFacing(face);
        this.dataTracker.set(VARIANT, variant.getId());
    }

    /**
     * Quick helper as two types spin
     */
    private static boolean doesRotate(int type) {
        return type == FrameType.GOLD.getId() || type == FrameType.REVERSED_GOLD.getId() || type == FrameType.DIAMOND.getId();
    }

    /**
     * Resets the rotation timer to 0
     */
    public void updateRotationTimer(boolean overturn) {
        this.rotationTimer = overturn ? -DIAMOND_TIMER : 0;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!player.isSneaking() && getFrameId() == FrameType.CLEAR.getId() && !getHeldItemStack().isEmpty()) {
            BlockPos behind = getBlockPos().offset(facing.getOpposite());
            BlockState state = getWorld().getBlockState(behind);
            if (!state.isAir()) {
                ActionResult result = state.onUse(getWorld(), player, hand, Util.createTraceResult(behind, facing, false));
                if (result.isAccepted()) {
                    return result;
                }
            }
        }
        return super.interact(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        // diamond spins on both sides
        int frameId = getFrameId();
        if (frameId == FrameType.DIAMOND.getId()) {
            rotationTimer++;
            // diamond winds down every 30 seconds, but does not go past 0, makes a full timer 3:30
            if (rotationTimer >= 300) {
                rotationTimer = 0;
                if (!level.isClientSide) {
                    int curRotation = getRotation();
                    if (curRotation > 0) {
                        this.setRotation(curRotation - 1);
                    }
                }
            }
            return;
        }
        // for gold and reversed gold, only increment timer serverside
        if (!level.isClientSide) {
            if (doesRotate(frameId)) {
                rotationTimer++;
                if (rotationTimer >= 20) {
                    rotationTimer = 0;
                    int curRotation = getRotation();
                    if (frameId == FrameType.REVERSED_GOLD.getId()) {
                        // modulo is not positive bounded, so we have to manually ensure positive
                        curRotation -= 1;
                        if (curRotation == -1) {
                            curRotation = 7;
                        }
                        this.setRotation(curRotation);
                    } else {
                        this.setRotation(curRotation + 1);
                    }
                }
            }
        }
    }

    @Override
    public void setHeldItemStack(ItemStack stack, boolean updateComparator) {
        super.setHeldItemStack(stack, updateComparator);
        // spinning frames reset to 0 on changing item
        if (updateComparator && !level.isClientSide && doesRotate(getFrameId())) {
            setRotation(0, false);
        }
    }

    /**
     * Internal logic to set the rotation
     */
    private void setRotationRaw(int rotationIn, boolean updateComparator) {
        this.getDataTracker().set(ROTATION, rotationIn);
        if (updateComparator) {
            this.world.updateComparators(this.attachmentPos, Blocks.AIR);
        }
    }

    @Override
    protected void setRotation(int rotationIn, boolean updateComparator) {
        this.rotationTimer = 0;
        // diamond goes 0-8 rotation, no modulo and needs to sync with client
        if (getFrameId() == FrameType.DIAMOND.getId()) {
            if (!level.isClientSide && updateComparator) {
                // play a sound as diamond is special
                this.playSound(Sounds.ITEM_FRAME_CLICK.getSound(), 1.0f, 1.0f);
            }
            // diamond allows rotation between 0 and 16
            setRotationRaw(Math.min(rotationIn, 16), updateComparator);
        } else {
            // non diamond rotates around after 7
            setRotationRaw(rotationIn % 8, updateComparator);
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    /**
     * Gets the frame type
     */
    public FrameType getFrameType() {
        return FrameType.byId(this.getFrameId());
    }

    /**
     * Gets the frame type
     */
    public Item getFrameItem() {
        return TinkerGadgets.itemFrame.get(getFrameType());
    }

    /**
     * Gets the index of the frame type
     */
    protected int getFrameId() {
        return this.dataTracker.get(VARIANT);
    }

    @Override
    protected ItemStack getAsItemStack() {
        return new ItemStack(getFrameItem());
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        ItemStack held = this.getHeldItemStack();
        if (held.isEmpty()) {
            return new ItemStack(getFrameItem());
        } else {
            return held.copy();
        }
    }

    @Override
    public boolean isFireImmune() {
        return super.isFireImmune() || getFrameId() == FrameType.NETHERITE.getId();
    }

    @Override
    public boolean isImmuneToExplosion() {
        return super.isImmuneToExplosion() || getFrameId() == FrameType.NETHERITE.getId();
    }

    @Override
    public int getComparatorPower() {
        if (this.getHeldItemStack().isEmpty()) {
            return 0;
        }
        int rotation = getRotation();
        if (getFrameId() == FrameType.DIAMOND.getId()) {
            return Math.min(15, rotation + 1);
        }
        return rotation % 8 + 1;
    }


    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        int frameId = this.getFrameId();
        compound.putInt(TAG_VARIANT, frameId);
        if (doesRotate(frameId)) {
            compound.putInt(TAG_ROTATION_TIMER, rotationTimer);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        super.readCustomDataFromNbt(compound);
        int frameId = compound.getInt(TAG_VARIANT);
        this.dataTracker.set(VARIANT, frameId);
        if (doesRotate(frameId)) {
            rotationTimer = compound.getInt(TAG_ROTATION_TIMER);
        }
    }

    @Override
    public void writeSpawnData(PacketByteBuf buffer) {
        buffer.writeVarInt(this.getFrameId());
        buffer.writeBlockPos(this.attachmentPos);
        buffer.writeVarInt(this.facing.getId());
    }

    @Override
    public void readSpawnData(PacketByteBuf buffer) {
        this.dataTracker.set(VARIANT, buffer.readVarInt());
        this.attachmentPos = buffer.readBlockPos();
        this.setFacing(Direction.byId(buffer.readVarInt()));
    }


    @Override
    protected Text getDefaultName() {
        return Text.translatable(getFrameItem().getTranslationKey());
    }
}
