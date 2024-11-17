package slimeknights.tconstruct.tables.block.entity.table;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;

import javax.annotation.Nonnull;

public abstract class RetexturedTableBlockEntity extends TableBlockEntity implements IRetexturedBlockEntity {
    private static final String TAG_TEXTURE = "texture";

    @NotNull
    @Getter
    private Block texture = Blocks.AIR;

    public RetexturedTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Text name, int size) {
        super(type, pos, state, name, size);
    }

    @Override
    public Box getRenderBoundingBox() {
        return new Box(this.pos, this.pos.add(1, 2, 1));
    }

    /* Textures */

    @NotNull
    @Override
    public ModelData getModelData() {
        return RetexturedHelper.getModelData(this.texture);
    }

    @Override
    public String getTextureName() {
        return RetexturedHelper.getTextureName(this.texture);
    }

    private void textureUpdated() {
        // update the texture in BE data
        if (this.world != null && this.world.isClient) {
            Block normalizedTexture = this.texture == Blocks.AIR ? null : this.texture;
            ModelData data = this.getModelData();
            if (data.get(RetexturedHelper.BLOCK_PROPERTY) != normalizedTexture) {
                requestModelDataUpdate();
                BlockState state = this.getCachedState();
                this.world.updateListeners(this.pos, state, state, 0);
            }
        }
    }

    @Override
    public void updateTexture(String name) {
        Block oldTexture = this.texture;
        this.texture = RetexturedHelper.getBlock(name);
        if (oldTexture != this.texture) {
            this.setChangedFast();
            this.textureUpdated();
        }
    }

    @Override
    public void saveSynced(NbtCompound tags) {
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
            this.textureUpdated();
        }
    }
}
