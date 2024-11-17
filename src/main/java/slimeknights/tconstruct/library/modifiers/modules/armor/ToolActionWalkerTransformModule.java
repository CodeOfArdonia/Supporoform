package slimeknights.tconstruct.library.modifiers.modules.armor;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolActionModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.MutableUseOnContext;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.List;

/**
 * Boot module that transforms walked on blocks using a tool action
 *
 * @param action    Transforming action
 * @param sound     Sound to play when transforming
 * @param radius    Radius to cover
 * @param condition Standard module condition
 */
public record ToolActionWalkerTransformModule(ToolAction action, SoundEvent sound, LevelingValue radius,
                                              ModifierCondition<IToolStackView> condition) implements ModifierModule, ArmorWalkRadiusModule<MutableUseOnContext>, ToolActionModifierHook, ConditionalModule<IToolStackView> {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ToolActionWalkerTransformModule>defaultHooks(ModifierHooks.BOOT_WALK, ModifierHooks.TOOL_ACTION);
    public static final RecordLoadable<ToolActionWalkerTransformModule> LOADER = RecordLoadable.create(
            Loadables.TOOL_ACTION.requiredField("tool_action", ToolActionWalkerTransformModule::action),
            Loadables.SOUND_EVENT.requiredField("sound", ToolActionWalkerTransformModule::sound),
            LevelingValue.LOADABLE.requiredField("radius", ToolActionWalkerTransformModule::radius),
            ModifierCondition.TOOL_FIELD,
            ToolActionWalkerTransformModule::new);

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public float getRadius(IToolStackView tool, ModifierEntry modifier) {
        return radius.compute(modifier.getLevel() + tool.getModifierLevel(TinkerModifiers.expanded.getId()));
    }

    @Override
    public void onWalk(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
        if (condition.matches(tool, modifier)) {
            ArmorWalkRadiusModule.super.onWalk(tool, modifier, living, prevPos, newPos);
        }
    }

    @Override
    public boolean canPerformAction(IToolStackView tool, ModifierEntry modifier, ToolAction toolAction) {
        return condition.matches(tool, modifier) && toolAction == this.action;
    }

    @Override
    public MutableUseOnContext getContext(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
        return new MutableUseOnContext(living.getWorld(), living instanceof PlayerEntity p ? p : null, Hand.MAIN_HAND, living.getEquippedStack(EquipmentSlot.FEET), Util.createTraceResult(newPos, Direction.UP, false));
    }

    @Override
    public void walkOn(IToolStackView tool, ModifierEntry entry, LivingEntity living, World world, BlockPos target, Mutable mutable, MutableUseOnContext context) {
        BlockState state = world.getBlockState(target);
        if (state.isReplaceable() || state.getBlock() instanceof PlantBlock) {
            mutable.set(target.getX(), target.getY() - 1, target.getZ());
            context.setOffsetPos(mutable);
            // transform the block
            BlockState original = world.getBlockState(mutable);
            BlockState transformed = original.getToolModifiedState(context, action, false);
            if (transformed != null) {
                world.setBlockState(mutable, transformed, Block.field_31022);
                world.breakBlock(target, true);
                world.playSound(null, mutable, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
                ToolDamageUtil.damageAnimated(tool, 1, living, EquipmentSlot.FEET);
            }
        }
    }

    @Override
    public RecordLoadable<ToolActionWalkerTransformModule> getLoader() {
        return LOADER;
    }


    /* Builder */

    /**
     * Creates a builder instance
     */
    public static Builder builder(ToolAction action, SoundEvent sound) {
        return new Builder(action, sound);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder extends ModuleBuilder.Stack<Builder> implements LevelingValue.Builder<ToolActionWalkerTransformModule> {
        private final ToolAction action;
        private final SoundEvent sound;

        @Override
        public ToolActionWalkerTransformModule amount(float flat, float eachLevel) {
            return new ToolActionWalkerTransformModule(action, sound, new LevelingValue(flat, eachLevel), condition);
        }
    }
}
