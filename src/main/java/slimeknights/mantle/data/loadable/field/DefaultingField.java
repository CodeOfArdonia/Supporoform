package slimeknights.mantle.data.loadable.field;

import com.google.gson.JsonObject;
import slimeknights.mantle.data.loadable.Loadable;

import java.util.function.Function;

/**
 * Optional field with a default value if missing
 *
 * @param <P> Parent object
 * @param <T> Loadable type
 */
public record DefaultingField<T, P>(Loadable<T> loadable, String key, T defaultValue, boolean serializeDefault,
                                    Function<P, T> getter) implements AlwaysPresentLoadableField<T, P> {
    @Override
    public T get(JsonObject json) {
        return this.loadable.getOrDefault(json, this.key, this.defaultValue);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
        T object = this.getter.apply(parent);
        if (this.serializeDefault || !this.defaultValue.equals(object)) {
            json.add(this.key, this.loadable.serialize(object));
        }
    }
}
