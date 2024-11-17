package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import org.joml.Quaternion;
import org.joml.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.entity.FluidEffectProjectile;
import slimeknights.tconstruct.tools.modifiers.ability.interaction.BlockingModifier;
import slimeknights.tconstruct.tools.modifiers.upgrades.ranged.ScopeModifier;

import static slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper.TANK_HELPER;

/**
 * Modifier that fires fluid as a projectile
 */
public class SpittingModifier extends Modifier implements GeneralInteractionModifierHook {
    @Override
    protected void registerHooks(Builder builder) {
        builder.addHook(this, ModifierHooks.GENERAL_INTERACT);
        builder.addModule(ToolTankHelper.TANK_HANDLER);
        builder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(FluidType.BUCKET_VOLUME));
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return BlockingModifier.blockWhileCharging(tool, UseAction.BOW);
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (!tool.isBroken() && source == InteractionSource.RIGHT_CLICK) {
            // launch if the fluid has effects, cannot simulate as we don't know the target yet
            FluidStack fluid = TANK_HELPER.getFluid(tool);
            if (fluid.getAmount() >= (1 + 2 * (modifier.getLevel() - 1)) && FluidEffectManager.INSTANCE.find(fluid.getFluid()).hasEffects()) {
                GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, 1.5f);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        ScopeModifier.scopingUsingTick(tool, entity, this.getUseDuration(tool, modifier) - timeLeft);
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        ScopeModifier.stopScoping(entity);
        if (!entity.getWorld().isClient) {
            int chargeTime = this.getUseDuration(tool, modifier) - timeLeft;
            if (chargeTime > 0) {
                // find the fluid to spit
                FluidStack fluid = TANK_HELPER.getFluid(tool);
                if (!fluid.isEmpty()) {
                    FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
                    if (recipe.hasEffects()) {
                        // projectile stats
                        float charge = GeneralInteractionModifierHook.getToolCharge(tool, chargeTime);
                        // power - size of each individual projectile
                        float power = charge * ConditionalStatModifierHook.getModifiedStat(tool, entity, ToolStats.PROJECTILE_DAMAGE);
                        // level acts like multishot level, meaning higher produces more projectiles
                        int level = modifier.intEffectiveLevel();
                        // amount is the amount per projectile, total cost is amount times level (every other shot is free)
                        // if its 0, that means we have only a couple mb left
                        long amount = Math.min(fluid.getAmount(), (int) (recipe.getAmount(fluid.getFluid()) * power) * level) / level;
                        if (amount > 0) {
                            // other stats now that we know we are shooting
                            // velocity determines how far it goes, does not impact damage unlike bows
                            float velocity = ConditionalStatModifierHook.getModifiedStat(tool, entity, ToolStats.VELOCITY) * charge * 3.0f;
                            float inaccuracy = ModifierUtil.getInaccuracy(tool, entity);

                            // multishot stuff
                            int shots = 1 + 2 * (level - 1);
                            float startAngle = ModifiableLauncherItem.getAngleStart(shots);
                            int primaryIndex = shots / 2;
                            for (int shotIndex = 0; shotIndex < shots; shotIndex++) {
                                FluidEffectProjectile spit = new FluidEffectProjectile(entity.getWorld(), entity, new FluidStack(fluid, amount), power);

                                // setup projectile target
                                Vector3f targetVector = new Vector3f(entity.getRotationVec(1.0f));
                                float angle = startAngle + (10 * shotIndex);
                                targetVector.transform(new Quaternion(new Vector3f(entity.getOppositeRotationVector(1.0f)), angle, true));
                                spit.setVelocity(targetVector.x(), targetVector.y(), targetVector.z(), velocity, inaccuracy);

                                // store all modifiers on the spit
                                spit.getCapability(EntityModifierCapability.CAPABILITY).ifPresent(cap -> cap.setModifiers(tool.getModifiers()));

                                // fetch the persistent data for the arrow as modifiers may want to store data
                                NamespacedNBT arrowData = PersistentDataCapability.getOrWarn(spit);
                                // let modifiers set properties
                                for (ModifierEntry entry : tool.getModifierList()) {
                                    entry.getHook(ModifierHooks.PROJECTILE_LAUNCH).onProjectileLaunch(tool, entry, entity, spit, null, arrowData, shotIndex == primaryIndex);
                                }

                                // finally, fire the projectile
                                entity.getWorld().spawnEntity(spit);
                                entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_LLAMA_SPIT, SoundCategory.PLAYERS, 1.0F, 1.0F / (entity.world.getRandom().nextFloat() * 0.4F + 1.2F) + charge * 0.5F + (angle / 10f));

                            }

                            // consume the fluid and durability
                            fluid.shrink(amount * level);
                            TANK_HELPER.setFluid(tool, fluid);
                            ToolDamageUtil.damageAnimated(tool, shots, entity, entity.getActiveHand());
                        }
                    }
                }
            }
        }
    }

}