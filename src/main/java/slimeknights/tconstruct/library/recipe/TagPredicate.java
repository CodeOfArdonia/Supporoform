package slimeknights.tconstruct.library.recipe;

import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;

import java.util.function.Predicate;

/**
 * Extended implementation of {@link net.minecraft.predicate.NbtPredicate} that supports syncing over the network
 */
public record TagPredicate(@Nullable NbtCompound tag) implements Predicate<NbtCompound> {
    /**
     * Loadable instance
     */
    public static final RecordLoadable<TagPredicate> LOADABLE = NBTLoadable.ALLOW_STRING.flatXmap(TagPredicate::new, p -> p.tag);
    /**
     * Instance that matches any NBT
     */
    public static final TagPredicate ANY = new TagPredicate(null);

    @Override
    public boolean test(@Nullable NbtCompound toTest) {
        return NbtHelper.matches(this.tag, toTest, true);
    }
}
