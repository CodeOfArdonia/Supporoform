package slimeknights.tconstruct.gadgets.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.item.TooltipItem;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.gadgets.capability.PiggybackCapability;
import slimeknights.tconstruct.gadgets.capability.PiggybackHandler;
import slimeknights.tconstruct.library.client.Icons;
import slimeknights.tconstruct.library.client.RenderUtils;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class PiggyBackPackItem extends TooltipItem {
    private static final int MAX_ENTITY_STACK = 3; // how many entities can be carried at once

    public PiggyBackPackItem(Settings props) {
        super(props);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        // is the chest slot empty?
        ItemStack chestArmor = playerIn.getEquippedStack(EquipmentSlot.CHEST);

        // need enough space to exchange the chest armor
        if (chestArmor.getItem() != this && playerIn.getInventory().getEmptySlot() == -1) {
            // not enough inventory space
            return ActionResult.PASS;
        }

        // try carrying the entity
        if (this.pickupEntity(playerIn, target)) {
            // unequip old armor
            if (chestArmor.getItem() != this) {
                ItemHandlerHelper.giveItemToPlayer(playerIn, chestArmor);
                chestArmor = ItemStack.EMPTY;
            }

            // we could pick it up just fine, check if we need to "equip" more of the item
            if (chestArmor.isEmpty()) {
                playerIn.equipStack(EquipmentSlot.CHEST, stack.split(1));
            } else if (chestArmor.getCount() < this.getEntitiesCarriedCount(playerIn)) {
                stack.split(1);
                chestArmor.increment(1);
            }
            // successfully picked up an entity
            return ActionResult.SUCCESS;
        }

        return ActionResult.CONSUME;
    }

    private boolean pickupEntity(PlayerEntity player, Entity target) {
        if (player.getEntityWorld().isClient || target.getType().isIn(TinkerTags.EntityTypes.PIGGYBACKPACK_BLACKLIST)) {
            return false;
        }
        // silly players, clicking on entities they're already carrying or riding
        if (target.getVehicle() == player || player.getVehicle() == target) {
            return false;
        }

        int count = 0;
        Entity toRide = player;
        while (toRide.hasPassengers() && count < MAX_ENTITY_STACK) {
            toRide = toRide.getPassengerList().get(0);
            count++;
            // don't allow more than 1 player, that can easily cause endless loops with riding detection for some reason.
            if (toRide instanceof PlayerEntity && target instanceof PlayerEntity) {
                return false;
            }
        }

        // can only ride one entity each
        if (!toRide.hasPassengers() && count < MAX_ENTITY_STACK) {
            // todo: possibly throw off all passengers of the target
            if (target.startRiding(toRide, true)) {
                if (player instanceof ServerPlayerEntity) {
                    TinkerNetwork.getInstance().sendVanillaPacket(player, new EntityPassengersSetS2CPacket(player));
                }
                return true;
            }
        }
        return false;
    }

    private int getEntitiesCarriedCount(LivingEntity player) {
        int count = 0;
        Entity ridden = player;
        while (ridden.hasPassengers()) {
            count++;
            ridden = ridden.getPassengerList().get(0);
        }

        return count;
    }

    public void matchCarriedEntitiesToCount(LivingEntity player, int maxCount) {
        int count = 0;
        // get top rider
        Entity ridden = player;
        while (ridden.hasPassengers()) {
            ridden = ridden.getPassengerList().get(0);
            count++;

            if (count > maxCount) {
                ridden.stopRiding();
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (entityIn instanceof LivingEntity livingEntity && livingEntity.getEquippedStack(EquipmentSlot.CHEST) == stack && entityIn.hasPassengers()) {
            int amplifier = this.getEntitiesCarriedCount(livingEntity) - 1;
            livingEntity.addStatusEffect(new StatusEffectInstance(TinkerGadgets.carryEffect.get(), 2, amplifier, true, false));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot equipmentSlot) {
        return ImmutableMultimap.of(); // no attributes, the potion effect handles them
    }

    public static class CarryPotionEffect extends TinkerEffect {
        static final String UUID = "ff4de63a-2b24-11e6-b67b-9e71128cae77";

        public CarryPotionEffect() {
            super(StatusEffectCategory.NEUTRAL, true);

            this.addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, UUID, -0.05D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            return true; // check every tick
        }

        @Override
        public void applyUpdateEffect(@NotNull LivingEntity livingEntityIn, int p_76394_2_) {
            ItemStack chestArmor = livingEntityIn.getEquippedStack(EquipmentSlot.CHEST);
            if (chestArmor.isEmpty() || chestArmor.getItem() != TinkerGadgets.piggyBackpack.get()) {
                TinkerGadgets.piggyBackpack.get().matchCarriedEntitiesToCount(livingEntityIn, 0);
            } else {
                TinkerGadgets.piggyBackpack.get().matchCarriedEntitiesToCount(livingEntityIn, chestArmor.getCount());
                if (!livingEntityIn.getEntityWorld().isClient) {
                    livingEntityIn.getCapability(PiggybackCapability.PIGGYBACK, null).ifPresent(PiggybackHandler::updatePassengers);
                }
            }
        }

        // TODO: proper sprite sheet for effect icons?
        @Override
        public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
            consumer.accept(new IClientMobEffectExtensions() {
                private void renderIcon(StatusEffectInstance effect, MatrixStack matrices, int x, int y) {
                    RenderUtils.setup(Icons.ICONS);
                    ElementScreen element = switch (effect.getAmplifier()) {
                        case 0 -> Icons.PIGGYBACK_1;
                        case 1 -> Icons.PIGGYBACK_2;
                        default -> Icons.PIGGYBACK_3;
                    };

                    element.draw(matrices, x + 6, y + 7);
                }

                @Override
                public boolean renderInventoryIcon(StatusEffectInstance effect, AbstractInventoryScreen<?> gui, MatrixStack matrices, int x, int y, int z) {
                    renderIcon(effect, matrices, x, y);
                    return true;
                }

                @Override
                public boolean renderGuiIcon(StatusEffectInstance effect, InGameHud gui, MatrixStack matrices, int x, int y, float z, float alpha) {
                    renderIcon(effect, matrices, x, y);
                    return true;
                }
            });
        }
    }
}
