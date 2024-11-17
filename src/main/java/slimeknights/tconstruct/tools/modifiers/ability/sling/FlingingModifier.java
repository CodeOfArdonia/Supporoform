package slimeknights.tconstruct.tools.modifiers.ability.sling;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.SlimeBounceHandler;

/**
 * Add velocity opposite of the targeted block
 */
public class FlingingModifier extends SlingModifier {
    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        super.onStoppedUsing(tool, modifier, entity, timeLeft);
        if (entity.isOnGround() && entity instanceof PlayerEntity player) {
            // check if player was targeting a block
            BlockHitResult mop = ModifiableItem.blockRayTrace(entity.getWorld(), player, RaycastContext.FluidHandling.NONE);
            if (mop.getType() == HitResult.Type.BLOCK) {
                // we fling the inverted player look vector
                float f = this.getForce(tool, modifier, entity, timeLeft, true) * 4;
                if (f > 0) {
                    Vec3d vec = player.getRotationVector().normalize();
                    float inaccuracy = ModifierUtil.getInaccuracy(tool, player) * 0.0075f;
                    Random random = player.getRandom();
                    player.addVelocity((vec.x + random.nextGaussian() * inaccuracy) * -f,
                            (vec.y + random.nextGaussian() * inaccuracy) * -f / 3f,
                            (vec.z + random.nextGaussian() * inaccuracy) * -f);
                    SlimeBounceHandler.addBounceHandler(player);
                    if (!entity.getWorld().isClient) {
                        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), Sounds.SLIME_SLING.getSound(), player.getSoundCategory(), 1, 1);
                        player.addExhaustion(0.2F);
                        player.getItemCooldownManager().set(tool.getItem(), 3);
                        ToolDamageUtil.damageAnimated(tool, 1, entity);
                    }
                    return;
                }
            }
        }
        entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), Sounds.SLIME_SLING.getSound(), entity.getSoundCategory(), 1, 0.5f);
    }
}
