package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.CircleAOEIterator;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.particle.FluidParticleData;
import slimeknights.tconstruct.tools.TinkerModifiers;

import static slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper.TANK_HELPER;
import static slimeknights.tconstruct.library.tools.helper.ModifierUtil.asLiving;

/**
 * Modifier to handle spilling recipes on interaction
 */
public class SplashingModifier extends Modifier implements EntityInteractionModifierHook, BlockInteractionModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
        hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(FluidType.BUCKET_VOLUME));
        hookBuilder.addHook(this, ModifierHooks.ENTITY_INTERACT, ModifierHooks.BLOCK_INTERACT);
    }

    @Override
    public ActionResult beforeEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Entity target, Hand hand, InteractionSource source) {
        // melee items get spilling via attack, non melee interact to use it
        if (tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            FluidStack fluid = TANK_HELPER.getFluid(tool);
            if (!fluid.isEmpty()) {
                FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
                if (recipe.hasEntityEffects()) {
                    if (!player.getWorld().isClient) {
                        // for the main target, consume fluids
                        float level = modifier.getEffectiveLevel();
                        int numTargets = 0;
                        int consumed = recipe.applyToEntity(fluid, level, new FluidEffectContext.Entity(player.getWorld(), player, player, null, target, asLiving(target)), FluidAction.EXECUTE);
                        if (consumed > 0) {
                            numTargets++;
                            UseFluidOnHitModifier.spawnParticles(target, fluid);
                        }

                        // expanded logic, they do not consume fluid, you get some splash for free
                        float range = 1 + tool.getModifierLevel(TinkerModifiers.expanded.get());
                        float rangeSq = range * range;
                        for (Entity aoeTarget : player.getWorld().getNonSpectatingEntities(Entity.class, target.getBoundingBox().expand(range, 0.25, range))) {
                            if (aoeTarget != player && aoeTarget != target && !(aoeTarget instanceof ArmorStandEntity stand && stand.isMarker()) && target.squaredDistanceTo(aoeTarget) < rangeSq) {
                                int aoeConsumed = recipe.applyToEntity(fluid, level, new FluidEffectContext.Entity(player.getWorld(), player, player, null, aoeTarget, asLiving(aoeTarget)), FluidAction.EXECUTE);
                                if (aoeConsumed > 0) {
                                    numTargets++;
                                    UseFluidOnHitModifier.spawnParticles(aoeTarget, fluid);
                                    // consume the largest amount requested from any entity
                                    if (aoeConsumed > consumed) {
                                        consumed = aoeConsumed;
                                    }
                                }
                            }
                        }

                        // consume the fluid last, if any target used fluid
                        if (!player.isCreative()) {
                            if (consumed > 0) {
                                fluid.shrink(consumed);
                                TANK_HELPER.setFluid(tool, fluid);
                            }

                            // damage the tool, we charge for the multiplier and for the number of targets hit
                            ToolDamageUtil.damageAnimated(tool, MathHelper.ceil(numTargets * level), player, hand);
                        }
                    }

                    // cooldown based on attack speed/draw speed. both are on the same scale and default to 1, we don't care which one the tool uses
                    player.getItemCooldownManager().set(tool.getItem(), (int) (20 / ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.DRAW_SPEED)));
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    /**
     * Spawns particles at the given entity
     */
    private static void spawnParticles(World level, BlockHitResult hit, FluidStack fluid) {
        if (level instanceof ServerWorld) {
            Vec3d location = hit.getPos();
            ((ServerWorld) level).spawnParticles(new FluidParticleData(TinkerCommons.fluidParticle.get(), fluid), location.getX(), location.getY(), location.getZ(), 10, 0.1, 0.2, 0.1, 0.2);
        }
    }

    @Override
    public ActionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        if (tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            FluidStack fluid = TANK_HELPER.getFluid(tool);
            if (!fluid.isEmpty()) {
                FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
                if (recipe.hasEntityEffects()) {
                    PlayerEntity player = context.getPlayer();
                    World world = context.getWorld();
                    if (!context.getWorld().isClient) {
                        Direction face = context.getSide();
                        BlockPos pos = context.getBlockPos();
                        float level = modifier.getEffectiveLevel();
                        int numTargets = 0;
                        BlockHitResult hit = context.getHitResult();
                        int consumed = recipe.applyToBlock(fluid, level, new FluidEffectContext.Block(world, player, null, hit), FluidAction.EXECUTE);
                        if (consumed > 0) {
                            numTargets++;
                            spawnParticles(world, hit, fluid);
                        }

                        // AOE selection logic, get boosted from both fireprimer (unique modifer) and expanded
                        int range = tool.getModifierLevel(TinkerModifiers.expanded.getId());
                        if (range > 0 && player != null) {
                            for (BlockPos offset : CircleAOEIterator.calculate(tool, ItemStack.EMPTY, world, player, pos, face, 1 + range, false, AreaOfEffectIterator.AOEMatchType.TRANSFORM)) {
                                BlockHitResult offsetHit = hit.withBlockPos(offset);
                                int aoeConsumed = recipe.applyToBlock(fluid, level, new FluidEffectContext.Block(world, player, null, offsetHit), FluidAction.EXECUTE);
                                if (aoeConsumed > 0) {
                                    numTargets++;
                                    spawnParticles(world, offsetHit, fluid);
                                    if (aoeConsumed > consumed) {
                                        consumed = aoeConsumed;
                                    }
                                }
                            }
                        }

                        // consume the fluid last, if any target used fluid
                        if (player == null || !player.isCreative()) {
                            if (consumed > 0) {
                                fluid.shrink(consumed);
                                TANK_HELPER.setFluid(tool, fluid);
                            }

                            // damage the tool, we charge for the multiplier and for the number of targets hit
                            ItemStack stack = context.getStack();
                            if (ToolDamageUtil.damage(tool, MathHelper.ceil(numTargets * level), player, stack) && player != null) {
                                player.sendEquipmentBreakStatus(source.getSlot(context.getHand()));
                            }
                        }
                    }

                    // cooldown based on attack speed/draw speed. both are on the same scale and default to 1, we don't care which one the tool uses
                    if (player != null) {
                        player.getItemCooldownManager().set(tool.getItem(), (int) (20 / ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.DRAW_SPEED)));
                    }
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }
}
