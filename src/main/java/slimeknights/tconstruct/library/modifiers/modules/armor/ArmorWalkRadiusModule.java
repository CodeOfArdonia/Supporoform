package slimeknights.tconstruct.library.modifiers.modules.armor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ArmorWalkModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/**
 * Implementation of the standard radius walk behavior used by most implementations
 *
 * @param <T> Context class
 */
public interface ArmorWalkRadiusModule<T> extends ArmorWalkModifierHook, ModifierModule {
    List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ArmorWalkRadiusModule<?>>defaultHooks(ModifierHooks.BOOT_WALK);

    @Override
    default List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    /**
     * Gets the radius for this modifier
     */
    float getRadius(IToolStackView tool, ModifierEntry modifier);

    /**
     * Called to modify a position
     *
     * @param tool    Tool instance
     * @param entry   Modifier level
     * @param living  Entity walking
     * @param world   World being walked in
     * @param target  Position target for effect
     * @param mutable Mutable position you can freely modify
     * @param context Extra data context used by the modifier
     */
    void walkOn(IToolStackView tool, ModifierEntry entry, LivingEntity living, World world, BlockPos target, Mutable mutable, T context);

    /**
     * Creates additional context to pass into the on walk method
     */
    @SuppressWarnings("ConstantConditions") // it won't be null if you actually intend to use it
    default T getContext(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
        return null;
    }

    @Override
    default void onWalk(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
        if (living.isOnGround() && !tool.isBroken() && !living.getWorld().isClient) {
            T context = this.getContext(tool, modifier, living, prevPos, newPos);
            float radius = Math.min(16, this.getRadius(tool, modifier));
            Mutable mutable = new Mutable();
            World world = living.getWorld();
            Vec3d posVec = living.getPos();
            BlockPos center = BlockPos.ofFloored(posVec.x, posVec.y + 0.5, posVec.z);
            for (BlockPos pos : BlockPos.iterate(center.add(BlockPos.ofFloored(-radius, 0, -radius)), center.add(BlockPos.ofFloored(radius, 0, radius)))) {
                if (pos.isWithinDistance(living.getPos(), radius)) {
                    this.walkOn(tool, modifier, living, world, pos, mutable, context);
                    if (tool.isBroken()) {
                        break;
                    }
                }
            }
        }
    }
}
