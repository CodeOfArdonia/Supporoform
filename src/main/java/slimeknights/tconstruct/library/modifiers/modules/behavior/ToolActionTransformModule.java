package slimeknights.tconstruct.library.modifiers.modules.behavior;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolActionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.special.BlockTransformModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * Module which transforms a block using a tool action
 */
public record ToolActionTransformModule(ToolAction action, SoundEvent sound, boolean requireGround, int eventId,
                                        ModifierCondition<IToolStackView> condition) implements BlockTransformModule, ToolActionModifierHook, ConditionalModule<IToolStackView> {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ToolActionTransformModule>defaultHooks(ModifierHooks.BLOCK_INTERACT, ModifierHooks.TOOL_ACTION);
    public static final RecordLoadable<ToolActionTransformModule> LOADER = RecordLoadable.create(
            Loadables.TOOL_ACTION.requiredField("tool_action", ToolActionTransformModule::action),
            Loadables.SOUND_EVENT.requiredField("sound", ToolActionTransformModule::sound),
            BooleanLoadable.INSTANCE.requiredField("require_ground", ToolActionTransformModule::requireGround),
            IntLoadable.FROM_MINUS_ONE.defaultField("event_id", -1, ToolActionTransformModule::eventId),
            ModifierCondition.TOOL_FIELD,
            ToolActionTransformModule::new);

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public boolean canPerformAction(IToolStackView tool, ModifierEntry modifier, ToolAction toolAction) {
        return this.condition.matches(tool, modifier) && this.action == toolAction;
    }

    @Override
    public boolean transform(IToolStackView tool, ItemUsageContext context, BlockState original, boolean playSound) {
        World level = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockPos above = pos.up();

        // hoes and shovels: air or plants above
        if (this.requireGround) {
            BlockState state = level.getBlockState(above);
            if (!state.isReplaceable() && !(state.getBlock() instanceof PlantBlock)) {
                return false;
            }
        }

        // normal action transform
        PlayerEntity player = context.getPlayer();
        BlockState transformed = original.getToolModifiedState(context, this.action, false);
        if (transformed != null) {
            if (playSound) {
                level.playSound(player, pos, this.sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
                if (this.eventId != -1) {
                    level.syncWorldEvent(player, this.eventId, pos, 0);
                }
            }
            if (!level.isClient) {
                level.setBlockState(pos, transformed, Block.field_31022);
                if (this.requireGround) {
                    level.breakBlock(above, true);
                }
                BlockTransformModifierHook.afterTransformBlock(tool, context, original, pos, this.action);
            }
            return true;
        }
        return false;
    }

    @Override
    public RecordLoadable<ToolActionTransformModule> getLoader() {
        return LOADER;
    }


    /* Builder */

    public static Builder builder(ToolAction action, SoundEvent sound) {
        return new Builder(action, sound);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder extends ModuleBuilder.Stack<Builder> {
        private final ToolAction action;
        private final SoundEvent sound;
        private boolean requireGround;
        /**
         * Event ID to play upon success
         *
         * @see World#syncWorldEvent(int, BlockPos, int)
         */
        @Setter
        @Accessors(fluent = true)
        private int eventId = -1;

        /**
         * Sets the module to require the block above to be empty
         */
        public Builder requireGround() {
            this.requireGround = true;
            return this;
        }

        /**
         * Builds the module
         */
        public ToolActionTransformModule build() {
            return new ToolActionTransformModule(this.action, this.sound, this.requireGround, this.eventId, this.condition);
        }
    }
}
