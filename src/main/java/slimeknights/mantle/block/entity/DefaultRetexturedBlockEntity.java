package slimeknights.mantle.block.entity;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.util.RetexturedHelper;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Standard implementation for {@link IRetexturedBlockEntity}, use alongside {@link RetexturedBlock} and {@link slimeknights.mantle.item.RetexturedBlockItem}
 */
public class DefaultRetexturedBlockEntity extends MantleBlockEntity implements IRetexturedBlockEntity {
    @NotNull
    @Getter
    private Block texture = Blocks.AIR;

    public DefaultRetexturedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        return RetexturedHelper.getModelData(this.texture);
    }

    @Override
    public String getTextureName() {
        return RetexturedHelper.getTextureName(this.texture);
    }

    @Override
    public void updateTexture(String name) {
        Block oldTexture = this.texture;
        this.texture = RetexturedHelper.getBlock(name);
        if (oldTexture != this.texture) {
            this.setChangedFast();
            RetexturedHelper.onTextureUpdated(this);
        }
    }

    @Override
    protected boolean shouldSyncOnUpdate() {
        return true;
    }

    @Override
    protected void saveSynced(NbtCompound tags) {
        super.saveSynced(tags);
        if (this.texture != Blocks.AIR) {
            tags.putString(TAG_TEXTURE, this.getTextureName());
        }
    }

    @Override
    public void readNbt(NbtCompound tags) {
        super.readNbt(tags);
        if (tags.contains(TAG_TEXTURE, NbtElement.STRING_TYPE)) {
            this.texture = RetexturedHelper.getBlock(tags.getString(TAG_TEXTURE));
            RetexturedHelper.onTextureUpdated(this);
        }
    }
}
