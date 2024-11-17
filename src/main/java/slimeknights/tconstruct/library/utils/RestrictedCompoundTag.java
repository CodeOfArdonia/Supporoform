package slimeknights.tconstruct.library.utils;

import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.BiFunction;

/**
 * Wrapper around a compound tag to restrict access
 */
@RequiredArgsConstructor
public class RestrictedCompoundTag {
    /**
     * Base NBT compound
     */
    private final NbtCompound tag;
    /**
     * List of tags with restricted access
     */
    private final Set<String> restrictedKeys;

    /**
     * Checks if the data contains the given tag
     *
     * @param name Namespaced key
     * @param type Tag type, see {@link NbtElement} for values
     * @return True if the tag is contained
     */
    public boolean contains(String name, int type) {
        return !this.restrictedKeys.contains(name) && this.tag.contains(name, type);
    }


    /* Get functions */

    /**
     * Gets a namespaced key from NBT
     *
     * @param name     Name
     * @param function Function to get data using the key
     * @param <T>      NBT type of output
     * @return Data based on the function
     */
    protected <T> T get(String name, BiFunction<NbtCompound, String, T> function, T defaultValue) {
        if (this.restrictedKeys.contains(name)) {
            return defaultValue;
        }
        return function.apply(this.tag, name);
    }


    /**
     * Reads an generic NBT value from the mod data
     *
     * @param name Name
     * @return Integer value
     */
    @SuppressWarnings("ConstantConditions")
    @Nullable
    public NbtElement get(String name) {
        return this.get(name, NbtCompound::get, null);
    }

    /**
     * Reads an integer from the tag
     *
     * @param name Name
     * @return Integer value
     */
    public int getInt(String name) {
        return this.get(name, NbtCompound::getInt, 0);
    }

    /**
     * Reads an boolean from the tag
     *
     * @param name Name
     * @return Boolean value
     */
    public boolean getBoolean(String name) {
        return this.get(name, NbtCompound::getBoolean, false);
    }

    /**
     * Reads an float from the tag
     *
     * @param name Name
     * @return Float value
     */
    public float getFloat(String name) {
        return this.get(name, NbtCompound::getFloat, 0f);
    }

    /**
     * Reads a string from the tag
     *
     * @param name Name
     * @return String value
     */
    public String getString(String name) {
        return this.get(name, NbtCompound::getString, "");
    }

    /**
     * Reads a compound from the tag
     *
     * @param name Name
     * @return Compound value
     */
    public NbtCompound getCompound(String name) {
        if (this.restrictedKeys.contains(name)) {
            return new NbtCompound();
        }
        return this.tag.getCompound(name);
    }

    /**
     * Reads a list from the tag
     *
     * @param name Name
     * @return Compound value
     */
    public NbtList getList(String name, int type) {
        if (this.restrictedKeys.contains(name)) {
            return new NbtList();
        }
        return this.tag.getList(name, type);
    }


    /* Put methods */

    /**
     * Sets the given NBT into tag
     *
     * @param name Key name
     * @param nbt  NBT value
     */
    public void put(String name, NbtElement nbt) {
        if (!this.restrictedKeys.contains(name)) {
            this.tag.put(name, nbt);
        }
    }

    /**
     * Sets an integer from the tag
     *
     * @param name  Name
     * @param value Integer value
     */
    public void putInt(String name, int value) {
        if (!this.restrictedKeys.contains(name)) {
            this.tag.putInt(name, value);
        }
    }

    /**
     * Sets an boolean from the tag
     *
     * @param name  Name
     * @param value Boolean value
     */
    public void putBoolean(String name, boolean value) {
        if (!this.restrictedKeys.contains(name)) {
            this.tag.putBoolean(name, value);
        }
    }

    /**
     * Sets an float from the tag
     *
     * @param name  Name
     * @param value Float value
     */
    public void putFloat(String name, float value) {
        if (!this.restrictedKeys.contains(name)) {
            this.tag.putFloat(name, value);
        }
    }

    /**
     * Reads a string from the tag
     *
     * @param name  Name
     * @param value String value
     */
    public void putString(String name, String value) {
        if (!this.restrictedKeys.contains(name)) {
            this.tag.putString(name, value);
        }
    }

    /**
     * Removes the given key from tag
     *
     * @param name Key to remove
     */
    public void remove(String name) {
        if (!this.restrictedKeys.contains(name)) {
            this.tag.remove(name);
        }
    }
}
