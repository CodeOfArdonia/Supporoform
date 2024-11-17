package slimeknights.mantle.data.predicate.entity;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.TagKey;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.RegistryPredicateRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.SingletonLoader;

import java.util.List;
import java.util.function.Predicate;

/**
 * Predicate matching an entity
 */
public interface LivingEntityPredicate extends IJsonPredicate<LivingEntity> {
    /**
     * Predicate that matches all entities
     */
    LivingEntityPredicate ANY = simple(entity -> true);
    /**
     * Loader for block state predicates
     */
    RegistryPredicateRegistry<EntityType<?>, LivingEntity> LOADER = new RegistryPredicateRegistry<>("Entity Predicate", ANY, Loadables.ENTITY_TYPE, Entity::getType, "entities", Loadables.ENTITY_TYPE_TAG, (tag, entity) -> entity.getType().isIn(tag));

    /**
     * Gets an inverted condition
     */
    @Override
    default IJsonPredicate<LivingEntity> inverted() {
        return LOADER.invert(this);
    }


    /* Singletons */

    /**
     * Predicate that matches water sensitive entities
     */
    LivingEntityPredicate WATER_SENSITIVE = simple(LivingEntity::hurtByWater);
    /**
     * Predicate that matches fire immune entities
     */
    LivingEntityPredicate FIRE_IMMUNE = simple(Entity::isFireImmune);
    /**
     * Predicate that matches fire immune entities
     */
    LivingEntityPredicate ON_FIRE = simple(Entity::isOnFire);
    /**
     * Checks if the entity is on the ground
     */
    LivingEntityPredicate ON_GROUND = simple(Entity::isOnGround);
    /**
     * Entities that are in the air
     */
    LivingEntityPredicate CROUCHING = simple(Entity::isInSneakingPose);

    // water
    /**
     * Entities with eyes in water
     */
    LivingEntityPredicate EYES_IN_WATER = simple(Entity::isSubmergedInWater);
    /**
     * Entities with feet in water
     */
    LivingEntityPredicate FEET_IN_WATER = simple(Entity::isTouchingWater);
    /**
     * Entities with head and feet are in water
     */
    LivingEntityPredicate UNDERWATER = simple(Entity::isSubmergedInWater);
    /**
     * Checks if the entity is being hit by rain at their location
     */
    LivingEntityPredicate RAINING = simple(entity -> entity.getWorld().hasRain(entity.getBlockPos()));

    /**
     * Creates a new predicate singleton
     */
    static LivingEntityPredicate simple(Predicate<LivingEntity> predicate) {
        return SingletonLoader.singleton(loader -> new LivingEntityPredicate() {
            @Override
            public boolean matches(LivingEntity entity) {
                return predicate.test(entity);
            }

            @Override
            public IGenericLoader<? extends LivingEntityPredicate> getLoader() {
                return loader;
            }
        });
    }


    /* Helper methods */

    /**
     * Creates an entity set predicate
     */
    static IJsonPredicate<LivingEntity> set(EntityType<?>... types) {
        return LOADER.setOf(ImmutableSet.copyOf(types));
    }

    /**
     * Creates a tag predicate
     */
    static IJsonPredicate<LivingEntity> tag(TagKey<EntityType<?>> tag) {
        return LOADER.tag(tag);
    }

    /**
     * Creates an and predicate
     */
    @SafeVarargs
    static IJsonPredicate<LivingEntity> and(IJsonPredicate<LivingEntity>... predicates) {
        return LOADER.and(List.of(predicates));
    }

    /**
     * Creates an or predicate
     */
    @SafeVarargs
    static IJsonPredicate<LivingEntity> or(IJsonPredicate<LivingEntity>... predicates) {
        return LOADER.or(List.of(predicates));
    }
}
