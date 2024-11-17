package slimeknights.mantle.data.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.Identifier;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.JsonHelper;

import java.util.function.Function;

/**
 * Predicate registry that upon failure to find a predicate type will fallback to the fallback type
 */
public class FallbackPredicateRegistry<T, F> extends PredicateRegistry<T> {
    private final Function<T, F> getter;
    private final PredicateRegistry<F> fallback;
    private final RecordLoadable<FallbackPredicate> fallbackLoader;

    /**
     * Creates a new instance
     *
     * @param defaultInstance Default instance, typically expected to be an any predicate.
     */
    public FallbackPredicateRegistry(String name, IJsonPredicate<T> defaultInstance, PredicateRegistry<F> fallback, Function<T, F> getter, String fallbackName) {
        super(name, defaultInstance);
        this.fallback = fallback;
        this.getter = getter;
        this.fallbackLoader = RecordLoadable.create(fallback.directField(fallbackName + "_type", p -> p.predicate), FallbackPredicate::new);
        this.register(Mantle.getResource(fallbackName), this.fallbackLoader);
    }

    /**
     * Creates a fallback predicate instance
     */
    public IJsonPredicate<T> fallback(IJsonPredicate<F> predicate) {
        return new FallbackPredicate(predicate);
    }

    @Override
    public IJsonPredicate<T> convert(JsonElement element, String key) {
        if (element.isJsonNull()) {
            return this.getDefault();
        }
        // identify type key, and the object we will load from
        JsonObject object;
        Identifier type;
        if (element.isJsonObject()) {
            object = element.getAsJsonObject();
            type = JsonHelper.getResourceLocation(object, "type");
        } else if (this.compact && element.isJsonPrimitive()) {
            EMPTY_OBJECT.entrySet().clear();
            object = EMPTY_OBJECT;
            type = JsonHelper.convertToResourceLocation(element, "type");
        } else {
            throw new JsonSyntaxException("Invalid " + this.getName() + " JSON at " + key + ", must be a JSON object" + (this.compact ? " or a string" : ""));
        }
        //  see if we have a primary loader, if so parse that
        IGenericLoader<? extends IJsonPredicate<T>> loader = this.loaders.getValue(type);
        if (loader != null) {
            return loader.deserialize(object);
        }
        // primary loader failed, try a fallback loader
        return new FallbackPredicate(this.fallback.convert(element, key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonElement serialize(IJsonPredicate<T> src) {
        // write the fallback directly to JSON instead of as a nested type
        if (src instanceof NestedPredicate<?>) {
            return this.fallback.serialize(((NestedPredicate<F>) src).predicate());
        }
        return super.serialize(src);
    }

    /**
     * Helper interface to make the cast work
     */
    private interface NestedPredicate<F> {
        IJsonPredicate<F> predicate();
    }

    /**
     * Predicate matching another predicate type
     */
    @RequiredArgsConstructor
    public class FallbackPredicate implements IJsonPredicate<T>, NestedPredicate<F> {
        private final IJsonPredicate<F> predicate;

        @Override
        public IJsonPredicate<F> predicate() {
            return this.predicate;
        }

        @Override
        public boolean matches(T input) {
            return this.predicate.matches(FallbackPredicateRegistry.this.getter.apply(input));
        }

        @Override
        public IJsonPredicate<T> inverted() {
            return FallbackPredicateRegistry.this.invert(this);
        }

        @Override
        public IGenericLoader<? extends IJsonPredicate<T>> getLoader() {
            return FallbackPredicateRegistry.this.fallbackLoader;
        }
    }
}
