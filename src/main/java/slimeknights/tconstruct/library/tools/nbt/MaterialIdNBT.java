package slimeknights.tconstruct.library.tools.nbt;

import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Similar to {@link MaterialNBT}, but does not check materials against the registry.
 * Used for rendering so we can have client side only materials for UIs. Anything logic based should use the regular material NBT
 */
@Getter
@EqualsAndHashCode
@ToString
public class MaterialIdNBT {
    /**
     * Instance containing no materials, for errors with parsing NBT
     */
    public final static MaterialIdNBT EMPTY = new MaterialIdNBT(ImmutableList.of());

    /**
     * List of materials contained in this NBT
     */
    private final List<MaterialVariantId> materials;

    /**
     * Creates a new material NBT
     */
    public MaterialIdNBT(List<? extends MaterialVariantId> materials) {
        this.materials = ImmutableList.copyOf(materials);
    }

    /**
     * Gets the material at the given index
     *
     * @param index Index
     * @return Material, or unknown if index is invalid
     */
    public MaterialVariantId getMaterial(int index) {
        if (index >= materials.size() || index < 0) {
            return IMaterial.UNKNOWN_ID;
        }
        return materials.get(index);
    }

    /**
     * Resolves all redirects, replacing with material redirects
     */
    public MaterialIdNBT resolveRedirects() {
        boolean changed = false;
        ImmutableList.Builder<MaterialVariantId> builder = ImmutableList.builder();
        IMaterialRegistry registry = MaterialRegistry.getInstance();
        for (MaterialVariantId id : materials) {
            MaterialId original = id.getId();
            MaterialId resolved = registry.resolve(original);
            if (resolved != original) {
                changed = true;
            }
            builder.add(MaterialVariantId.create(resolved, id.getVariant()));
        }
        // return a new instance only if things changed
        if (changed) {
            return new MaterialIdNBT(builder.build());
        }
        return this;
    }

    /**
     * Parses the material list from NBT
     *
     * @param nbt NBT instance
     * @return MaterialNBT instance
     */
    public static MaterialIdNBT readFromNBT(@Nullable NbtElement nbt) {
        if (nbt == null || nbt.getType() != NbtElement.LIST_TYPE) {
            return EMPTY;
        }
        NbtList listNBT = (NbtList) nbt;
        if (listNBT.getHeldType() != NbtElement.STRING_TYPE) {
            return EMPTY;
        }

        List<MaterialVariantId> materials = listNBT.stream()
                .map(NbtElement::asString)
                .map(MaterialVariantId::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new MaterialIdNBT(materials);
    }

    /**
     * Writes this material list to NBT
     *
     * @return List of materials
     */
    public NbtList serializeToNBT() {
        return materials.stream()
                .map(MaterialVariantId::toString)
                .map(NbtString::of)
                .collect(Collectors.toCollection(NbtList::new));
    }

    /**
     * Parses the material list from a stack
     *
     * @param stack Tool stack instance
     * @return MaterialNBT instance
     */
    public static MaterialIdNBT from(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            return readFromNBT(nbt.getList(ToolStack.TAG_MATERIALS, NbtElement.STRING_TYPE));
        }
        return EMPTY;
    }

    /**
     * Writes this material list to the given stack
     */
    public ItemStack updateStack(ItemStack stack) {
        stack.getOrCreateNbt().put(ToolStack.TAG_MATERIALS, serializeToNBT());
        return stack;
    }

    /**
     * Writes this material list to the given stack
     */
    @SuppressWarnings("UnusedReturnValue")
    public NbtCompound updateNBT(NbtCompound nbt) {
        nbt.put(ToolStack.TAG_MATERIALS, serializeToNBT());
        return nbt;
    }
}
