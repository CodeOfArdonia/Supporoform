package slimeknights.tconstruct.tools.modifiers.traits.skull;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.KeybindInteractModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

public class FirebreathModifier extends NoLevelsModifier implements KeybindInteractModifierHook {
    @Override
    public boolean startInteract(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, EquipmentSlot slot, TooltipKey keyModifier) {
        // stopped by water and by cooldown
        if (!player.isSneaking() && !player.hasStatusEffect(TinkerModifiers.fireballCooldownEffect.get()) && !player.isWet()) {
            // if not creative, this costs a fire charge
            boolean hasFireball = true;
            if (!player.isCreative()) {
                hasFireball = false;
                PlayerInventory inventory = player.getInventory();
                for (int i = 0; i < inventory.size(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    if (!stack.isEmpty() && stack.isIn(TinkerTags.Items.FIREBALLS)) {
                        hasFireball = true;
                        if (!player.getWorld().isClient) {
                            stack.decrement(1);
                            if (stack.isEmpty()) {
                                inventory.setStack(i, ItemStack.EMPTY);
                            }
                        }
                        break;
                    }
                }
            }
            // if we found a fireball, fire it
            if (hasFireball) {
                player.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0F, (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2F + 1.0F);
                if (!player.getWorld().isClient) {
                    Vec3d lookVec = player.getRotationVector().multiply(2.0f, 2.0f, 2.0f);
                    SmallFireballEntity fireball = new SmallFireballEntity(player.world, player, lookVec.x + player.getRandom().nextGaussian() / 16, lookVec.y, lookVec.z + player.getRandom().nextGaussian() / 16);
                    fireball.setPosition(fireball.getX(), player.getBodyY(0.5D) + 0.5D, fireball.getZ());
                    player.world.addFreshEntity(fireball);
                    TinkerModifiers.fireballCooldownEffect.get().apply(player, 100, 0, true);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ARMOR_INTERACT);
    }
}
