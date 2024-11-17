package slimeknights.mantle.data.predicate;

import lombok.RequiredArgsConstructor;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.DefaultingLoaderRegistry;

import java.util.List;

/**
 * Extension of generic loader registry providing default implementations for common predicates
 */
public class PredicateRegistry<T> extends DefaultingLoaderRegistry<IJsonPredicate<T>> {
    /**
     * Loader for inverted predicates
     */
    private final RecordLoadable<InvertedJsonPredicate> invertedLoader;
    /**
     * Loader for and predicates
     */
    private final RecordLoadable<AndJsonPredicate> andLoader;
    /**
     * Loader for or predicates
     */
    private final RecordLoadable<OrJsonPredicate> orLoader;

    /**
     * Creates a new instance
     *
     * @param name            Name to display in error messages
     * @param defaultInstance Default instance, typically expected to be an any predicate. Will be used for nulls and missing fields
     */
    public PredicateRegistry(String name, IJsonPredicate<T> defaultInstance) {
        super(name, defaultInstance, true);
        // create common types
        Loadable<List<IJsonPredicate<T>>> list = this.list(2);
        this.invertedLoader = RecordLoadable.create(this.directField("inverted_type", p -> p.predicate), InvertedJsonPredicate::new);
        this.andLoader = RecordLoadable.create(list.requiredField("predicates", p -> p.children), AndJsonPredicate::new);
        this.orLoader = RecordLoadable.create(list.requiredField("predicates", p -> p.children), OrJsonPredicate::new);
        // register common types
        this.register(Mantle.getResource("any"), defaultInstance.getLoader());
        this.register(Mantle.getResource("inverted"), this.invertedLoader);
        this.register(Mantle.getResource("and"), this.andLoader);
        this.register(Mantle.getResource("or"), this.orLoader);
    }

    /**
     * Inverts the given predicate
     *
     * @param predicate Predicate to invert
     * @return Inverted predicate
     */
    public IJsonPredicate<T> invert(IJsonPredicate<T> predicate) {
        return new InvertedJsonPredicate(predicate);
    }

    /**
     * Ands the given predicates together
     *
     * @param predicates Predicate list
     * @return Predicate that is true if all the passed predicates are true
     */
    public IJsonPredicate<T> and(List<IJsonPredicate<T>> predicates) {
        return new AndJsonPredicate(predicates);
    }

    /**
     * Ors the given predicates together
     *
     * @param predicates Predicate list
     * @return Predicate that is true if any of the passed predicates are true
     */
    public IJsonPredicate<T> or(List<IJsonPredicate<T>> predicates) {
        return new OrJsonPredicate(predicates);
    }


    /**
     * Predicate that inverts the condition.
     */
    @RequiredArgsConstructor
    public class InvertedJsonPredicate implements IJsonPredicate<T> {
        private final IJsonPredicate<T> predicate;

        @Override
        public boolean matches(T input) {
            return !this.predicate.matches(input);
        }

        @Override
        public IGenericLoader<? extends IJsonPredicate<T>> getLoader() {
            return PredicateRegistry.this.invertedLoader;
        }

        @Override
        public IJsonPredicate<T> inverted() {
            return this.predicate;
        }
    }

    /**
     * Predicate that requires all children to match
     */
    @RequiredArgsConstructor
    public class AndJsonPredicate implements IJsonPredicate<T> {
        private final List<IJsonPredicate<T>> children;

        @Override
        public boolean matches(T input) {
            for (IJsonPredicate<T> child : this.children) {
                if (!child.matches(input)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public IJsonPredicate<T> inverted() {
            return PredicateRegistry.this.invert(this);
        }

        @Override
        public IGenericLoader<? extends IJsonPredicate<T>> getLoader() {
            return PredicateRegistry.this.andLoader;
        }
    }

    /**
     * Predicate that requires any child to match
     */
    @RequiredArgsConstructor
    public class OrJsonPredicate implements IJsonPredicate<T> {
        private final List<IJsonPredicate<T>> children;

        @Override
        public boolean matches(T input) {
            for (IJsonPredicate<T> child : this.children) {
                if (child.matches(input)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public IJsonPredicate<T> inverted() {
            return PredicateRegistry.this.invert(this);
        }

        @Override
        public IGenericLoader<? extends IJsonPredicate<T>> getLoader() {
            return PredicateRegistry.this.orLoader;
        }
    }
}
