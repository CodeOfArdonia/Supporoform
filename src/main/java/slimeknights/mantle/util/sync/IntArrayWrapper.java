package slimeknights.mantle.util.sync;

import lombok.AllArgsConstructor;
import net.minecraft.screen.PropertyDelegate;

import java.util.function.Supplier;

/**
 * Int array that wraps an integer array supplier
 */
@AllArgsConstructor
public class IntArrayWrapper implements PropertyDelegate {
    private final Supplier<int[]> sup;

    @Override
    public int get(int index) {
        int[] array = this.sup.get();
        if (index >= 0 && index < array.length) {
            return array[index];
        }
        return 0;
    }

    @Override
    public void set(int index, int value) {
        int[] array = this.sup.get();
        if (index >= 0 && index < array.length) {
            array[index] = value;
        }
    }

    @Override
    public int size() {
        return this.sup.get().length;
    }
}
