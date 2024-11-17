package slimeknights.mantle.block.entity;

import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for containers that can be renamed. Used in {@link slimeknights.mantle.block.InventoryBlock} to set the name on placement
 */
public interface INameableMenuProvider extends NamedScreenHandlerFactory, Nameable {

    /**
     * Gets the default name of this tile entity
     *
     * @return Default name
     */
    Text getDefaultName();

    /**
     * Gets the custom name for this tile entity
     *
     * @return Custom name
     */
    @Override
    @Nullable
    Text getCustomName();

    /**
     * Sets the name for this tile entity
     *
     * @param name New custom name
     */
    void setCustomName(Text name);

    @Override
    default Text getName() {
        Text customTitle = this.getCustomName();
        return customTitle != null ? customTitle : this.getDefaultName();
    }

    @Override
    default Text getDisplayName() {
        return this.getName();
    }
}
