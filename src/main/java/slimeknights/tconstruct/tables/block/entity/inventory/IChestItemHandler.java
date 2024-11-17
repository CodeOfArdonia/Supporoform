package slimeknights.tconstruct.tables.block.entity.inventory;

import net.minecraft.nbt.NbtCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandlerModifiable;
import slimeknights.mantle.block.entity.MantleBlockEntity;

/**
 * Interface for tinker chest TEs
 */
public interface IChestItemHandler extends IItemHandlerModifiable, INBTSerializable<NbtCompound>, IScalingContainer {
    /**
     * Sets the parent of this block
     */
    void setParent(MantleBlockEntity parent);
}
