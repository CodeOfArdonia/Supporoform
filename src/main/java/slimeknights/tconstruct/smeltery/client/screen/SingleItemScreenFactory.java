package slimeknights.tconstruct.smeltery.client.screen;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreens.Provider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.screen.BackgroundContainerScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.menu.SingleItemContainerMenu;

/**
 * Screen factory for the single item container, one container for multiple backgrounds
 */
public class SingleItemScreenFactory implements Provider<SingleItemContainerMenu, BackgroundContainerScreen<SingleItemContainerMenu>> {
    private static final int HEIGHT = 133;
    private static final Identifier DEFAULT = TConstruct.getResource("textures/gui/blank.png");

    /**
     * Gets the background path for the given tile
     *
     * @param tile Tile
     * @return Background path
     */
    private static Identifier getBackground(@Nullable BlockEntity tile) {
        if (tile != null) {
            Identifier id = Registries.BLOCK_ENTITY_TYPE.getId(tile.getType());
            if (id != null) {
                return new Identifier(id.getNamespace(), String.format("textures/gui/%s.png", id.getPath()));
            }
        }
        return DEFAULT;
    }

    @Override
    public BackgroundContainerScreen<SingleItemContainerMenu> create(SingleItemContainerMenu container, PlayerInventory inventory, Text name) {
        return new BackgroundContainerScreen<>(container, inventory, name, HEIGHT, getBackground(container.getTile()));
    }
}
