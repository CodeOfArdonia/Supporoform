package slimeknights.tconstruct.tools.modifiers.ability.armor.walker;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

public class FlamewakeModifier extends AbstractWalkerModifier {
    @Override
    protected float getRadius(IToolStackView tool, int level) {
        return 1.5f + tool.getModifierLevel(TinkerModifiers.expanded.getId());
    }

    @Override
    protected void walkOn(IToolStackView tool, int level, LivingEntity living, World world, BlockPos target, Mutable mutable) {
        // fire starting
        if (AbstractFireBlock.canPlaceAt(world, target, living.getHorizontalFacing())) {
            world.playSound(null, target, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, RANDOM.nextFloat() * 0.4F + 0.8F);
            world.setBlockState(target, AbstractFireBlock.getState(world, target), Block.field_31022
);
            ToolDamageUtil.damageAnimated(tool, 1, living, EquipmentSlot.FEET);
        }
    }
}
