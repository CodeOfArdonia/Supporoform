package slimeknights.tconstruct.smeltery.block.entity.module;

import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags.EntityTypes;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipeCache;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Module to handle fetching items from the bounds and interacting with entities in the structure
 */
@RequiredArgsConstructor
public class EntityMeltingModule {
    /**
     * Standard damage source for melting most mobs
     */
    public static final DamageSource SMELTERY_DAMAGE = new DamageSource(TConstruct.prefix("smeltery_heat")).setIsFire();
    /**
     * Special damage source for "absorbing" hot entities
     */
    public static final DamageSource SMELTERY_MAGIC = new DamageSource(TConstruct.prefix("smeltery_magic")).setMagic();

    private final MantleBlockEntity parent;
    private final IFluidHandler tank;
    /**
     * Supplier that returns true if the tank has space
     */
    private final BooleanSupplier canMeltEntities;
    /**
     * Function that tries to insert an item into the inventory
     */
    private final Function<ItemStack, ItemStack> insertFunction;
    /**
     * Function that returns the bounds to check for entities
     */
    private final Supplier<Box> bounds;

    @Nullable
    private EntityMeltingRecipe lastRecipe;

    /**
     * Gets a nonnull world instance from the parent
     */
    private World getLevel() {
        return Objects.requireNonNull(parent.getWorld(), "Parent tile entity has null world");
    }

    /**
     * Finds a recipe for the given entity type
     *
     * @param type Entity type
     * @return Recipe
     */
    @Nullable
    private EntityMeltingRecipe findRecipe(EntityType<?> type) {
        if (lastRecipe != null && lastRecipe.matches(type)) {
            return lastRecipe;
        }
        // find a new recipe if the last recipe does not match
        EntityMeltingRecipe recipe = EntityMeltingRecipeCache.findRecipe(getLevel().getRecipeManager(), type);
        if (recipe != null) {
            lastRecipe = recipe;
        }
        return recipe;
    }

    /**
     * Gets the default fluid result
     *
     * @return Default fluid
     */
    public static FluidStack getDefaultFluid() {
        // TODO: consider a way to put this in a recipe
        return new FluidStack(TinkerFluids.liquidSoul.get(), FluidValues.GLASS_PANE / 5);
    }

    /**
     * checks if an entity can be melted
     *
     * @param entity Entity to check
     * @return True if they can be melted
     */
    private boolean canMeltEntity(LivingEntity entity) {
        // fire based mobs are absorbed instead of damaged
        return !entity.isInvulnerableTo(entity.isFireImmune() ? SMELTERY_MAGIC : SMELTERY_DAMAGE)
                // have to special case players because for some dumb reason creative players do not return true to invulnerable to
                && !(entity instanceof PlayerEntity && ((PlayerEntity) entity).getAbilities().invulnerable)
                // also have to special case fire resistance, so a blaze with fire resistance is immune to the smeltery
                && !entity.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
    }

    /**
     * Interacts with entities in the structure
     *
     * @return True if something was melted and fuel is needed
     */
    public boolean interactWithEntities() {
        Box boundingBox = bounds.get();
        if (boundingBox == null) {
            return false;
        }

        Boolean canMelt = null;
        boolean melted = false;
        for (Entity entity : getLevel().getNonSpectatingEntities(Entity.class, boundingBox)) {
            if (!entity.isAlive()) {
                continue;
            }

            // items are placed inside the smeltery
            EntityType<?> type = entity.getType();
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = insertFunction.apply(itemEntity.getStack());
                // picked up whole stack
                if (stack.isEmpty()) {
                    entity.discard();
                } else {
                    itemEntity.setStack(stack);
                }
            }

            // only can melt living, ensure its not immune to our damage
            // if canMelt is already found as false, skip instance checks, we only care about items now
            // if the type is hidden, skip as well, I suppose thats your blacklist if you must have one
            else if (canMelt != Boolean.FALSE && !type.isIn(EntityTypes.MELTING_HIDE) && entity instanceof LivingEntity && canMeltEntity((LivingEntity) entity)) {
                // only fetch boolean once, its not the fastest as it tries to consume fuel
                if (canMelt == null) canMelt = canMeltEntities.getAsBoolean();

                // ensure we have fuel/any other needed smeltery states
                if (canMelt) {
                    // determine what we are melting
                    FluidStack fluid;
                    int damage;
                    EntityMeltingRecipe recipe = findRecipe(entity.getType());
                    if (recipe != null) {
                        fluid = recipe.getOutput((LivingEntity) entity);
                        damage = recipe.getDamage();
                    } else {
                        fluid = getDefaultFluid();
                        damage = 2;
                    }

                    // if the entity is successfully damaged, fill the tank with fluid
                    if (entity.damage(entity.isFireImmune() ? SMELTERY_MAGIC : SMELTERY_DAMAGE, damage)) {
                        // its fine if we don't fill it all, leftover fluid is just lost
                        this.tank.fill(fluid, FluidAction.EXECUTE);
                        melted = true;
                    }
                }
            }
        }
        return melted;
    }
}
