package slimeknights.tconstruct.tools.modifiers.ability.interaction;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraftforge.common.ToolAction;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolActionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.mining.RemoveBlockModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.behavior.ShowOffhandModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.CircleAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.Collections;

/**
 * Modifier that starts a fire at the given position
 */
@RequiredArgsConstructor
public class FirestarterModifier extends NoLevelsModifier implements EntityInteractionModifierHook, BlockInteractionModifierHook, ToolActionModifierHook, RemoveBlockModifierHook {
    /**
     * Generic action for the sake of people who want compat but do not want to request a specific action
     */
    private static final ToolAction LIGHT_FIRE = ToolAction.get("light_fire");
    /**
     * Compat with mods adding custom campfires
     */
    private static final ToolAction LIGHT_CAMPFIRE = ToolAction.get("light_campfire");

    @Getter
    private final int priority;

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ShowOffhandModule.DISALLOW_BROKEN);
        hookBuilder.addHook(this, ModifierHooks.ENTITY_INTERACT, ModifierHooks.BLOCK_INTERACT, ModifierHooks.TOOL_ACTION, ModifierHooks.REMOVE_BLOCK);
    }

    @Override
    public boolean shouldDisplay(boolean advanced) {
        return this.priority > Short.MIN_VALUE;
    }

    @Override
    public Text getDisplayName(IToolStackView tool, ModifierEntry entry) {
        return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, entry));
    }

    @Override
    public boolean canPerformAction(IToolStackView tool, ModifierEntry modifier, ToolAction toolAction) {
        return toolAction == LIGHT_CAMPFIRE || toolAction == LIGHT_FIRE;
    }

    @Override
    public ActionResult afterEntityUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, LivingEntity target, Hand hand, InteractionSource source) {
        if (tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source) && target instanceof CreeperEntity creeper) {
            player.getWorld().playSound(player, creeper.getX(), creeper.getY(), creeper.getZ(), SoundEvents.ITEM_FLINTANDSTEEL_USE, creeper.getSoundCategory(), 1.0F, RANDOM.nextFloat() * 0.4F + 0.8F);
            if (!player.getWorld().isClient) {
                creeper.ignite();
                ToolDamageUtil.damageAnimated(tool, 1, player, source.getSlot(hand));
            }
            return ActionResult.success(player.getWorld().isClient);
        }
        return ActionResult.PASS;
    }

    /**
     * Ignites the given block
     */
    private static boolean ignite(IToolStackView tool, World world, BlockPos pos, BlockState state, Direction sideHit, Direction horizontalFacing, @Nullable PlayerEntity player) {
        // campfires first
        if (CampfireBlock.canBeLit(state) || CandleBlock.canBeLit(state) || CandleCakeBlock.canBeLit(state)) {
            world.playSound(player, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, RANDOM.nextFloat() * 0.4F + 0.8F);
            world.setBlockState(pos, state.with(Properties.LIT, true), 11);
            world.emitGameEvent(player, GameEvent.BLOCK_PLACE, pos);
            return true;
        }

        // ignite the TNT
        if (state.getBlock() instanceof TntBlock tnt) {
            tnt.onCaughtFire(state, world, pos, sideHit, player);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
            return true;
        }

        // fire starting
        BlockPos offset = pos.offset(sideHit);
        if (AbstractFireBlock.canPlaceAt(world, offset, horizontalFacing)) {
            world.playSound(player, offset, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, RANDOM.nextFloat() * 0.4F + 0.8F);
            world.setBlockState(offset, AbstractFireBlock.getState(world, offset), 11);
            return true;
        }
        return false;
    }

    @Override
    public ActionResult beforeBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        if (tool.isBroken() || !tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            return ActionResult.PASS;
        }
        if (context.getWorld().getBlockState(context.getBlockPos()).isIn(BlockTags.CANDLE_CAKES)) {
            return this.afterBlockUse(tool, modifier, context, source);
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        if (tool.isBroken() || !tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            return ActionResult.PASS;
        }
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        Direction sideHit = context.getSide();
        BlockState state = world.getBlockState(pos);

        // if targeting fire, offset to behind the fire
        boolean targetingFire = false;
        if (state.isIn(BlockTags.FIRE)) {
            pos = pos.offset(sideHit.getOpposite());
            targetingFire = true;
        }

        // AOE selection logic, get boosted from both fireprimer (unique modifer) and expanded
        int range = tool.getModifierLevel(TinkerModifiers.fireprimer.getId()) + tool.getModifierLevel(TinkerModifiers.expanded.getId());
        Iterable<BlockPos> targets = Collections.emptyList();
        if (range > 0 && player != null) {
            targets = CircleAOEIterator.calculate(tool, ItemStack.EMPTY, world, player, pos, sideHit, 1 + range, true, AreaOfEffectIterator.AOEMatchType.TRANSFORM);
        }

        // burn it all in AOE
        Direction horizontalFacing = context.getHorizontalPlayerFacing();
        // first burn the center, unless we already know its fire
        boolean didIgnite = false;
        ItemStack stack = context.getStack();
        EquipmentSlot slotType = source.getSlot(context.getHand());
        if (!targetingFire) {
            didIgnite = ignite(tool, world, pos, state, sideHit, horizontalFacing, player);
            if (didIgnite && ToolDamageUtil.damage(tool, 1, player, stack)) {
                if (player != null) {
                    player.sendEquipmentBreakStatus(slotType);
                }
                return ActionResult.success(world.isClient);
            }
        }
        // ignite the edges, if any worked return success
        for (BlockPos target : targets) {
            if (ignite(tool, world, target, world.getBlockState(target), sideHit, horizontalFacing, player)) {
                didIgnite = true;
                if (ToolDamageUtil.damage(tool, 1, player, stack)) {
                    if (player != null) {
                        player.sendEquipmentBreakStatus(slotType);
                    }
                    break;
                }
            }
        }
        // when targeting fire, return true so left click interact does not continue to run
        return didIgnite || targetingFire ? ActionResult.success(world.isClient) : ActionResult.PASS;
    }

    @Nullable
    @Override
    public Boolean removeBlock(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        if (context.getState().is(Blocks.FIRE) && tool.getHook(ToolHooks.INTERACTION).canInteract(tool, this.getId(), InteractionSource.LEFT_CLICK)) {
            return false;
        }
        return null;
    }
}
