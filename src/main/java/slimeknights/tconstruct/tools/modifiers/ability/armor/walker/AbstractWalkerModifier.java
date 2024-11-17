package slimeknights.tconstruct.tools.modifiers.ability.armor.walker;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ArmorWalkModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public abstract class AbstractWalkerModifier extends NoLevelsModifier implements ArmorWalkModifierHook {
    /**
     * Gets the radius for this modifier
     */
    protected abstract float getRadius(IToolStackView tool, int level);

    /**
     * Called to modify a position
     *
     * @param tool    Tool instance
     * @param level   Modifier level
     * @param living  Entity walking
     * @param world   World being walked in
     * @param target  Position target for effect
     * @param mutable Mutable position you can freely modify
     */
    protected abstract void walkOn(IToolStackView tool, int level, LivingEntity living, World world, BlockPos target, Mutable mutable);

    @Override
    public void onWalk(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
        if (living.isOnGround() && !tool.isBroken() && !living.getWorld().isClient) {
            float radius = Math.min(16, this.getRadius(tool, modifier.getLevel()));
            Mutable mutable = new Mutable();
            World world = living.getWorld();
            Vec3d posVec = living.getPos();
            BlockPos center = BlockPos.ofFloored(posVec.x, posVec.y + 0.5, posVec.z);
            for (BlockPos pos : BlockPos.iterate(center.add(BlockPos.ofFloored(-radius, 0, -radius)), center.add(BlockPos.ofFloored(radius, 0, radius)))) {
                if (pos.isWithinDistance(living.getPos(), radius)) {
                    this.walkOn(tool, modifier.getLevel(), living, world, pos, mutable);
                    if (tool.isBroken()) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.BOOT_WALK);
    }
}
