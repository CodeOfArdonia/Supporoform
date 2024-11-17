package slimeknights.mantle.util.sync;

import lombok.AllArgsConstructor;
import net.minecraft.screen.Property;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Data slot implementation using lambdas for the getter and setter
 */
@AllArgsConstructor
public class LambdaDataSlot extends Property {
    private final IntSupplier getter;
    private final IntConsumer setter;

    /**
     * Constructor to let you start from a value other than 0
     */
    public LambdaDataSlot(int startingValue, IntSupplier getter, IntConsumer setter) {
        this(getter, setter);
        this.oldValue = startingValue;
    }

    @Override
    public int get() {
        return this.getter.getAsInt();
    }

    @Override
    public void set(int value) {
        this.setter.accept(value);
    }
}
