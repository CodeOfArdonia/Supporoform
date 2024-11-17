package slimeknights.mantle.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Extension of tile entity to make it namable
 */
@Getter
public abstract class NameableBlockEntity extends MantleBlockEntity implements INameableMenuProvider {
    private static final String TAG_CUSTOM_NAME = "CustomName";

    /**
     * Default title for this tile entity
     */
    private final Text defaultName;
    /**
     * Title set to this tile entity
     */
    @Setter
    private Text customName;

    public NameableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Text defaultTitle) {
        super(type, pos, state);
        this.defaultName = defaultTitle;
    }

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        if (tags.contains(TAG_CUSTOM_NAME, NbtElement.STRING_TYPE)) {
            this.customName = Text.Serializer.fromJson(tags.getString(TAG_CUSTOM_NAME));
        }
    }

    @Override
    public void saveSynced(NbtCompound tags) {
        super.saveSynced(tags);
        if (this.hasCustomName()) {
            tags.putString(TAG_CUSTOM_NAME, Text.Serializer.toJson(this.customName));
        }
    }
}
