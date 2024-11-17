package slimeknights.tconstruct.library.tools.nbt;

import slimeknights.tconstruct.library.tools.SlotType;

import java.util.function.BiFunction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * Read only view of {@link ModDataNBT}
 */
public interface IModDataView extends INamespacedNBTView {
    /**
     * Empty variant of mod data
     */
    IModDataView EMPTY = new IModDataView() {
        @Override
        public int getSlots(SlotType type) {
            return 0;
        }

        @Override
        public <T> T get(Identifier name, BiFunction<NbtCompound, String, T> function) {
            return function.apply(new NbtCompound(), name.toString());
        }

        @Override
        public boolean contains(Identifier name, int type) {
            return false;
        }
    };

    /**
     * Gets the number of slots provided by this data
     *
     * @param type Type of slot to get
     * @return Number of slots
     */
    int getSlots(SlotType type);
}
