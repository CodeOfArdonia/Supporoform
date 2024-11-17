package slimeknights.tconstruct.library.tools.nbt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

import java.util.function.BiFunction;

/**
 * NBT wrapper enforcing namespaces on compound keys
 */
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class NamespacedNBT implements INamespacedNBTView {
    /**
     * Compound representing modifier data
     */
    @Getter(AccessLevel.PROTECTED)
    private final NbtCompound data;

    /**
     * Creates a new mod data containing empty data
     */
    public NamespacedNBT() {
        this(new NbtCompound());
    }

    @Override
    public <T> T get(Identifier name, BiFunction<NbtCompound, String, T> function) {
        return function.apply(this.data, name.toString());
    }

    @Override
    public boolean contains(Identifier name, int type) {
        return this.data.contains(name.toString(), type);
    }

    /**
     * Sets the given NBT into the data
     *
     * @param name Key name
     * @param nbt  NBT value
     */
    public void put(Identifier name, NbtElement nbt) {
        this.data.put(name.toString(), nbt);
    }

    /**
     * Sets an integer from the mod data
     *
     * @param name  Name
     * @param value Integer value
     */
    public void putInt(Identifier name, int value) {
        this.data.putInt(name.toString(), value);
    }

    /**
     * Sets an boolean from the mod data
     *
     * @param name  Name
     * @param value Boolean value
     */
    public void putBoolean(Identifier name, boolean value) {
        this.data.putBoolean(name.toString(), value);
    }

    /**
     * Sets an float from the mod data
     *
     * @param name  Name
     * @param value Float value
     */
    public void putFloat(Identifier name, float value) {
        this.data.putFloat(name.toString(), value);
    }

    /**
     * Reads a string from the mod data
     *
     * @param name  Name
     * @param value String value
     */
    public void putString(Identifier name, String value) {
        this.data.putString(name.toString(), value);
    }

    /**
     * Removes the given key from the NBT
     *
     * @param name Key to remove
     */
    public void remove(Identifier name) {
        this.data.remove(name.toString());
    }


    /* Networking */

    /**
     * Gets a copy of the internal data, generally should only be used for syncing, no reason to call directly
     */
    public NbtCompound getCopy() {
        return this.data.copy();
    }

    /**
     * Called to merge this NBT data from another
     *
     * @param data data
     */
    public void copyFrom(NbtCompound data) {
        this.data.getKeys().clear();
        this.data.copyFrom(data);
    }

    /**
     * Parses the data from NBT
     *
     * @param data data
     * @return Parsed mod data
     */
    public static NamespacedNBT readFromNBT(NbtCompound data) {
        return new NamespacedNBT(data);
    }
}
