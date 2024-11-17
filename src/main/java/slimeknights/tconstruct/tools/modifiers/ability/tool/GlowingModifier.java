package slimeknights.tconstruct.tools.modifiers.ability.tool;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.mining.RemoveBlockModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.behavior.ShowOffhandModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.shared.TinkerCommons;

public class GlowingModifier extends NoLevelsModifier implements BlockInteractionModifierHook, RemoveBlockModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ShowOffhandModule.DISALLOW_BROKEN);
        hookBuilder.addHook(this, ModifierHooks.BLOCK_INTERACT, ModifierHooks.REMOVE_BLOCK);
    }

    @Override
    public int getPriority() {
        return 75; // after bucketing
    }

    @Override
    public Text getDisplayName(IToolStackView tool, ModifierEntry entry) {
        return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, entry));
    }

    @Override
    public ActionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        if (tool.getCurrentDurability() >= 10 && tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            PlayerEntity player = context.getPlayer();
            if (!context.getWorld().isClient) {
                World world = context.getWorld();
                Direction face = context.getSide();
                BlockPos pos = context.getBlockPos().offset(face);
                if (TinkerCommons.glow.get().addGlow(world, pos, face.getOpposite())) {
                    // damage the tool, showing animation if relevant
                    if (ToolDamageUtil.damage(tool, 10, player, context.getStack()) && player != null) {
                        player.sendEquipmentBreakStatus(source.getSlot(context.getHand()));
                    }
                    world.playSound(null, pos, world.getBlockState(pos).getSoundGroup(world, pos, player).getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
            }
            return ActionResult.success(context.getWorld().isClient);
        }
        return ActionResult.PASS;
    }

    @Nullable
    @Override
    public Boolean removeBlock(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
        if (context.getState().isOf(TinkerCommons.glow.get()) && tool.getHook(ToolHooks.INTERACTION).canInteract(tool, this.getId(), InteractionSource.LEFT_CLICK)) {
            return false;
        }
        return null;
    }
}
