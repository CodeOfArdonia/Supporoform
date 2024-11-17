package slimeknights.tconstruct.shared.block.entity;

import slimeknights.mantle.block.entity.InventoryBlockEntity;
import slimeknights.tconstruct.common.SoundUtils;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.tables.menu.TabbedContainerMenu;
import slimeknights.tconstruct.tables.network.UpdateStationScreenPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Tile entity that displays items in world. TODO: better name?
 */
public abstract class TableBlockEntity extends InventoryBlockEntity {
    /**
     * tick sound was last played for each player
     */
    private final Map<UUID, Integer> lastSoundTick = new HashMap<>();

    public TableBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, Text name, int inventorySize) {
        super(tileEntityTypeIn, pos, state, name, false, inventorySize);
    }

    public TableBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, Text name, int inventorySize, int maxStackSize) {
        super(tileEntityTypeIn, pos, state, name, false, inventorySize, maxStackSize);
    }

    /* Syncing */

    @Override
    public void setStack(int slot, ItemStack itemstack) {
        // send a slot update to the client when items change, so we can update the TESR
        if (world != null && world instanceof ServerWorld && !world.isClient && !ItemStack.areEqual(itemstack, getStack(slot))) {
            TinkerNetwork.getInstance().sendToClientsAround(new InventorySlotSyncPacket(itemstack, slot, pos), (ServerWorld) world, this.pos);
        }
        super.setStack(slot, itemstack);
    }

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = super.toInitialChunkDataNbt();
        // inventory is already in main NBT, include it in update tag
        writeInventoryToNBT(nbt);
        return nbt;
    }

    /**
     * Sends a packet to all players with this container open
     */
    public void syncToRelevantPlayers(Consumer<PlayerEntity> action) {
        if (this.world == null || this.world.isClient) {
            return;
        }

        this.world.getPlayers().stream()
                // sync if they are viewing this tile
                .filter(player -> {
                    if (player.currentScreenHandler instanceof TabbedContainerMenu) {
                        return ((TabbedContainerMenu<?>) player.currentScreenHandler).getTile() == this;
                    }
                    return false;
                })
                // send packets
                .forEach(action);
    }

    /**
     * Checks if we can play the sound right now
     */
    protected boolean isSoundReady(PlayerEntity player) {
        int lastSound = lastSoundTick.getOrDefault(player.getUuid(), 0);
        if (lastSound < player.age) {
            lastSoundTick.put(player.getUuid(), player.age);
            return true;
        }
        return false;
    }

    /**
     * Plays the crafting sound for all players around the given player
     *
     * @param player the player
     */
    protected void playCraftSound(PlayerEntity player) {
        if (isSoundReady(player)) {
            SoundUtils.playSoundForAll(player, Sounds.SAW.getSound(), 0.8f, 0.8f + 0.4f * player.getWorld().random.nextFloat());
        }
    }

    /**
     * Update the screen to the given player
     *
     * @param player Player to send an update to
     */
    protected void syncScreen(PlayerEntity player) {
        if (this.world != null && !this.world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            TinkerNetwork.getInstance().sendTo(UpdateStationScreenPacket.INSTANCE, serverPlayer);
        }
    }

    /**
     * Update the screen for all players using this UI
     */
    protected void syncScreenToRelevantPlayers() {
        if (this.world != null && !this.world.isClient) {
            syncToRelevantPlayers(this::syncScreen);
        }
    }
}
