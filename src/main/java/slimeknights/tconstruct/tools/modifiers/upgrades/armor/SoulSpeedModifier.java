package slimeknights.tconstruct.tools.modifiers.upgrades.armor;

import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.build.EnchantmentModule.Constant;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

public class SoulSpeedModifier extends Modifier implements TooltipModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(new Constant(Enchantments.SOUL_SPEED, 1));
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP);
    }

    /**
     * Gets the position this entity is standing on, cloned from protected living entity method
     */
    private static BlockPos getOnPosition(LivingEntity living) {
        Vec3d position = living.getPos();
        int x = MathHelper.floor(position.x);
        int y = MathHelper.floor(position.y - (double) 0.2F);
        int z = MathHelper.floor(position.z);
        BlockPos pos = new BlockPos(x, y, z);
        if (living.world.isEmptyBlock(pos)) {
            BlockPos below = pos.down();
            BlockState blockstate = living.world.getBlockState(below);
            if (blockstate.collisionExtendsVertically(living.world, below, living)) {
                return below;
            }
        }

        return pos;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey key, TooltipContext tooltipFlag) {
        // must either have no player or a player on soulsand
        if (player == null || key != TooltipKey.SHIFT || (!player.isFallFlying() && player.world.getBlockState(getOnPosition(player)).is(BlockTags.SOUL_SPEED_BLOCKS))) {
            // multiplies boost by 10 and displays as a percent as the players base movement speed is 0.1 and is in unknown units
            // percentages make sense
            TooltipModifierHook.addPercentBoost(this, this.getDisplayName(), 0.3f + modifier.getLevel() * 0.105f, tooltip);
        }
    }
}
