package slimeknights.tconstruct.tools.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraftforge.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.tools.logic.InteractionHandler;

/**
 * Packet sent client to server when an empty hand interaction
 */
@RequiredArgsConstructor
public enum InteractWithAirPacket implements IThreadsafePacket {
    /**
     * Right click with an empty main hand and a chestplate
     */
    MAINHAND(Hand.MAIN_HAND),
    /**
     * Right click with an empty off hand and a chestplate
     */
    OFFHAND(Hand.OFF_HAND),
    /**
     * Left click with a supported tool
     */
    LEFT_CLICK(Hand.MAIN_HAND);

    private final Hand hand;

    /**
     * Gets the packet for the given hand
     */
    public static InteractWithAirPacket fromChestplate(Hand hand) {
        return hand == Hand.OFF_HAND ? OFFHAND : MAINHAND;
    }

    /**
     * Gets the packet from the packet buffer
     */
    public static InteractWithAirPacket read(PacketByteBuf buffer) {
        return buffer.readEnumConstant(InteractWithAirPacket.class);
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        buffer.writeEnumConstant(this);
    }

    @Override
    public void handleThreadsafe(Context context) {
        ServerPlayerEntity player = context.getSender();
        if (player != null && !player.isSpectator()) {
            if (this == LEFT_CLICK) {
                ItemStack held = player.getStackInHand(this.hand);
                if (held.isIn(TinkerTags.Items.INTERACTABLE_LEFT)) {
                    ActionResult result = InteractionHandler.onLeftClickInteraction(player, held, this.hand);
                    if (result.shouldSwingHand()) {
                        player.swingHand(this.hand, true);
                    }
                }
            } else {
                ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
                if (chestplate.isIn(TinkerTags.Items.INTERACTABLE_ARMOR) && player.getStackInHand(this.hand).isEmpty()) {
                    ActionResult result = InteractionHandler.onChestplateUse(player, chestplate, this.hand);
                    if (result.shouldSwingHand()) {
                        player.swingHand(this.hand, true);
                    }
                }
            }
        }
    }
}
