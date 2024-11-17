package slimeknights.tconstruct.library.modifiers.fluid;

import com.iafenvoy.uranus.object.DamageUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static slimeknights.tconstruct.library.tools.helper.ModifierUtil.asLiving;
import static slimeknights.tconstruct.library.tools.helper.ModifierUtil.asPlayer;

/**
 * Context for calling fluid effects
 */
@Getter
@RequiredArgsConstructor
public abstract class FluidEffectContext {
    protected final World level;
    /**
     * Entity using the fluid
     */
    @Nullable
    protected final LivingEntity entity;
    /**
     * Player using the fluid, may be null if a non-player is the source of the fluid
     */
    @Nullable
    protected final PlayerEntity player;
    /**
     * Projectile that caused the fluid, null if no projectile is used (e.g. melee or interact effects)
     */
    @Nullable
    protected final ProjectileEntity projectile;

    /**
     * Gets a damage source based on this context
     */
    public DamageSource createDamageSource() {
        if (projectile != null) {
            return DamageUtil.build(entity, DamageTypes.MOB_PROJECTILE);
        }
        if (player != null) {
            return DamageUtil.build(entity, DamageTypes.PLAYER_ATTACK);
        }
        if (entity != null) {
            return DamageUtil.build(entity, DamageTypes.MOB_ATTACK);
        }
        // we should never reach here, but just in case
        return DamageUtil.build(entity, DamageTypes.GENERIC);
    }

    /**
     * Context for fluid effects targeting an entity
     */
    @Getter
    public static class Entity extends FluidEffectContext {
        private final net.minecraft.entity.Entity target;
        @Nullable
        private final LivingEntity livingTarget;

        public Entity(World level, @Nullable LivingEntity holder, @Nullable PlayerEntity player, @Nullable ProjectileEntity projectile, net.minecraft.entity.Entity target, @Nullable LivingEntity livingTarget) {
            super(level, holder, player, projectile);
            this.target = target;
            this.livingTarget = livingTarget;
        }

        public Entity(World level, @Nullable LivingEntity holder, @Nullable ProjectileEntity projectile, net.minecraft.entity.Entity target) {
            this(level, holder, asPlayer(holder), projectile, target, asLiving(target));
        }

        public Entity(World level, PlayerEntity player, @Nullable ProjectileEntity projectile, LivingEntity target) {
            this(level, player, player, projectile, target, target);
        }
    }

    /**
     * Context for fluid effects targeting an entity
     */
    public static class Block extends FluidEffectContext {
        @Getter
        private final BlockHitResult hitResult;
        private BlockState state;

        public Block(World level, @Nullable LivingEntity holder, @Nullable PlayerEntity player, @Nullable ProjectileEntity projectile, BlockHitResult hitResult) {
            super(level, holder, player, projectile);
            this.hitResult = hitResult;
        }

        public Block(World level, @Nullable LivingEntity holder, @Nullable ProjectileEntity projectile, BlockHitResult hitResult) {
            this(level, holder, asPlayer(holder), projectile, hitResult);
        }

        public Block(World level, @Nullable PlayerEntity player, @Nullable ProjectileEntity projectile, BlockHitResult hitResult) {
            this(level, player, player, projectile, hitResult);
        }

        /**
         * Gets the block state targeted by this context
         */
        public BlockState getBlockState() {
            if (state == null) {
                state = level.getBlockState(hitResult.getBlockPos());
            }
            return state;
        }

        /**
         * Checks if the block in front of the hit block is replaceable
         */
        public boolean isOffsetReplaceable() {
            return level.getBlockState(hitResult.getBlockPos().offset(hitResult.getSide())).isReplaceable();
        }
    }
}
