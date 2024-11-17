package slimeknights.tconstruct.tools.entity;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import slimeknights.tconstruct.tools.TinkerModifiers;

import static slimeknights.tconstruct.library.tools.helper.ModifierUtil.asLiving;

/**
 * Projectile that applies a fluid effect on hit, styled after llama spit.
 */
public class FluidEffectProjectile extends LlamaSpitEntity {
    private static final TrackedData<FluidStack> FLUID = DataTracker.registerData(FluidEffectProjectile.class, TinkerFluids.FLUID_DATA_SERIALIZER);

    @Setter
    private float power = 1;
    @Setter
    @Getter
    private int knockback = 1;

    public FluidEffectProjectile(EntityType<? extends FluidEffectProjectile> type, World level) {
        super(type, level);
    }

    public FluidEffectProjectile(World level, LivingEntity owner, FluidStack fluid, float power) {
        this(TinkerModifiers.fluidSpitEntity.get(), level);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setOwner(owner);
        this.setFluid(fluid);
        this.setPower(power);
    }

    /**
     * Gets the fluid for this spit
     */
    public FluidStack getFluid() {
        return this.dataTracker.get(FLUID);
    }

    /**
     * Sets the fluid for this spit
     */
    public void setFluid(FluidStack fluid) {
        this.dataTracker.set(FLUID, fluid);
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        Entity target = result.getEntity();
        // apply knockback to the entity regardless of fluid type
        if (this.knockback > 0) {
            Vec3d vec3 = this.getVelocity().multiply(1, 0, 1).normalize().multiply(this.knockback * 0.6);
            if (vec3.lengthSquared() > 0) {
                target.addVelocity(vec3.x, 0.1, vec3.z);
            }
        }
        FluidStack fluid = this.getFluid();
        if (!getWorld().isClient && !fluid.isEmpty()) {
            FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
            if (recipe.hasEntityEffects()) {
                int consumed = recipe.applyToEntity(fluid, this.power, new FluidEffectContext.Entity(this.getWorld(), asLiving(this.getOwner()), this, target), FluidAction.EXECUTE);
                // shrink our internal fluid, means we get a crossbow piercing like effect if its not all used
                // discarding when empty ensures the fluid won't continue with the block effect
                // unlike blocks, failing is fine, means we just continue through to the block below the entity
                fluid.shrink(consumed);
                if (fluid.isEmpty()) {
                    this.discard();
                } else {
                    this.setFluid(fluid);
                }
            }
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult hitResult) {
        // hit the block
        BlockPos hit = hitResult.getBlockPos();
        BlockState state = this.getWorld().getBlockState(hit);
        state.onProjectileHit(this.getWorld(), state, hitResult, this);
        // handle the fluid
        FluidStack fluid = this.getFluid();
        if (!getWorld().isClient) {
            if (!fluid.isEmpty()) {
                FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
                if (recipe.hasEntityEffects()) {
                    // run the effect until we run out of fluid or it fails
                    FluidEffectContext.Block context = new FluidEffectContext.Block(this.getWorld(), asLiving(this.getOwner()), this, hitResult);
                    int consumed;
                    do {
                        consumed = recipe.applyToBlock(fluid, this.power, context, FluidAction.EXECUTE);
                        fluid.shrink(consumed);
                    } while (consumed > 0 && !fluid.isEmpty());
                    // we can continue to live if we have fluid left and we broke our block, allows some neat shenanigans
                    // TODO: maybe use a more general check than air?
                    if (!fluid.isEmpty() && this.getWorld().getBlockState(hit).isAir()) {
                        return;
                    }
                }
            }
            this.discard();
        }
    }

    /* Network */

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(FLUID, FluidStack.EMPTY);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("power", this.power);
        nbt.putInt("knockback", this.knockback);
        FluidStack fluid = this.getFluid();
        if (!fluid.isEmpty()) {
            nbt.put("fluid", fluid.writeToNBT(new NbtCompound()));
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.power = nbt.getFloat("power");
        this.knockback = nbt.getInt("knockback");
        this.setFluid(FluidStack.loadFluidStackFromNBT(nbt.getCompound("fluid")));
    }
}
