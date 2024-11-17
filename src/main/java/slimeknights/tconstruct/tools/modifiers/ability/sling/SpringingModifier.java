package slimeknights.tconstruct.tools.modifiers.ability.sling;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.SlimeBounceHandler;

/**
 * Add velocity in the direction you face
 */
public class SpringingModifier extends SlingModifier {

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (!tool.isBroken() && source == InteractionSource.RIGHT_CLICK) {
            GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, 1f);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        super.onStoppedUsing(tool, modifier, entity, timeLeft);
        if (entity instanceof PlayerEntity player && !player.isFallFlying()) {
            player.addExhaustion(0.2F);

            float f = this.getForce(tool, modifier, player, timeLeft, true) * 1.05f;
            if (f > 0) {
                Vec3d look = player.getRotationVector().add(0, 1, 0).normalize();
                float inaccuracy = ModifierUtil.getInaccuracy(tool, player) * 0.0075f;
                Random random = player.getRandom();
                player.addVelocity(
                        (look.x + random.nextGaussian() * inaccuracy) * f,
                        (look.y + random.nextGaussian() * inaccuracy) * f / 2f,
                        (look.z + random.nextGaussian() * inaccuracy) * f);

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
        entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), Sounds.SLIME_SLING.getSound(), entity.getSoundCategory(), 1, 0.5f);
    }
}
