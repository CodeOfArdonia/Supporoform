package slimeknights.mantle.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.util.RetexturedHelper;

/**
 * Standard interface that should be used by retexturable tile entities, allows control over where the texture is saved.
 * Note that in the future, more of these methods will be made abstract, discouraging the use of {@link #getPersistentData()} ()} to store the texture (as we can sync our own tag easier)
 * <p>
 * Use alongside {@link RetexturedBlock} and {@link slimeknights.mantle.item.RetexturedBlockItem}. See {@link DefaultRetexturedBlockEntity} for implementation.
 */
public interface IRetexturedBlockEntity {
    /* Gets the Forge tile data for the tile entity */
    NbtCompound getPersistentData();

    /**
     * Gets the current texture block name. Encouraged to override this to not use {@link #getPersistentData()}
     *
     * @return Texture block name
     */
    default String getTextureName() {
        return RetexturedHelper.getTextureName(this.getPersistentData());
    }

    /**
     * Gets the current texture block
     *
     * @return Texture block
     */
    default Block getTexture() {
        return RetexturedHelper.getBlock(this.getTextureName());
    }

    /**
     * Updates the texture to the given name. Encouraged to override this to not use {@link #getPersistentData()}
     *
     * @param name Texture name
     */
    default void updateTexture(String name) {
        String oldName = this.getTextureName();
        RetexturedHelper.setTexture(this.getPersistentData(), name);
        if (!oldName.equals(name)) {
            // this is an unchecked cast, but no one should be using this interface not on a block entity
            RetexturedHelper.onTextureUpdated((BlockEntity) this);
        }
    }
}
