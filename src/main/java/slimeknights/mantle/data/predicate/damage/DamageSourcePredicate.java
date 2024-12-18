package slimeknights.mantle.data.predicate.damage;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.PredicateRegistry;
import slimeknights.mantle.data.predicate.TagPredicateRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;

import java.util.List;
import java.util.function.Predicate;

import static slimeknights.mantle.data.registry.GenericLoaderRegistry.SingletonLoader.singleton;

/**
 * Predicate testing for damage sources
 */
public interface DamageSourcePredicate extends IJsonPredicate<DamageSource> {
    /**
     * Predicate that matches all sources
     */
    DamageSourcePredicate ANY = simple(source -> true);
    /**
     * Loader for item predicates
     */
    PredicateRegistry<DamageSource> LOADER = new TagPredicateRegistry<>("Damage Source Predicate", ANY, Loadables.DAMAGE_TYPE_TAG, (tag, source) -> source.isIn(tag));

    /**
     * Damage that protection works against
     */
    DamageSourcePredicate CAN_PROTECT = simple(source -> !source.isIn(DamageTypeTags.BYPASSES_ENCHANTMENTS) && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY));

    @Override
    default IJsonPredicate<DamageSource> inverted() {
        return LOADER.invert(this);
    }

    /**
     * Creates a simple predicate with no parameters
     */
    static DamageSourcePredicate simple(Predicate<DamageSource> predicate) {
        return singleton(loader -> new DamageSourcePredicate() {
            @Override
            public boolean matches(DamageSource source) {
                return predicate.test(source);
            }

            @Override
            public IGenericLoader<? extends DamageSourcePredicate> getLoader() {
                return loader;
            }
        });
    }


    /* Helper methods */

    /**
     * Creates an and predicate
     */
    @SafeVarargs
    static IJsonPredicate<DamageSource> and(IJsonPredicate<DamageSource>... predicates) {
        return LOADER.and(List.of(predicates));
    }

    /**
     * Creates an or predicate
     */
    @SafeVarargs
    static IJsonPredicate<DamageSource> or(IJsonPredicate<DamageSource>... predicates) {
        return LOADER.or(List.of(predicates));
    }
}
