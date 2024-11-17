package slimeknights.mantle.util.sync;

import net.minecraft.screen.Property;
import net.minecraft.screen.PropertyDelegate;

import java.util.function.Consumer;

/**
 * Int reference holder that starts the "lastKnownValue" at an invalid value.
 * Fixes a bug where a non-zero value on the client is not updated on UI open as the new value is 0
 */
@SuppressWarnings("unused")
public class ValidZeroDataSlot extends Property {
    private final PropertyDelegate data;
    private final int idx;

    public ValidZeroDataSlot(PropertyDelegate data, int idx) {
        this.oldValue = Integer.MIN_VALUE;
        this.data = data;
        this.idx = idx;
    }

    @Override
    public int get() {
        return this.data.get(this.idx);
    }

    @Override
    public void set(int value) {
        this.data.set(this.idx, value);
    }

    /**
     * Creates smart int reference holders and adds them to the given consumer
     *
     * @param consumer Consumer for reference holders
     * @param array    Array source
     */
    public static void trackIntArray(Consumer<Property> consumer, PropertyDelegate array) {
        for (int i = 0; i < array.size(); ++i) {
            consumer.accept(new ValidZeroDataSlot(array, i));
        }
    }
}
