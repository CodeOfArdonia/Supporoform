package slimeknights.tconstruct.tables;

import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.model.block.TableModel;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;
import slimeknights.tconstruct.tables.client.TableTileEntityRenderer;
import slimeknights.tconstruct.tables.client.inventory.CraftingStationScreen;
import slimeknights.tconstruct.tables.client.inventory.ModifierWorktableScreen;
import slimeknights.tconstruct.tables.client.inventory.PartBuilderScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerChestScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class TableClientEvents extends ClientEventBase {
    @SubscribeEvent
    static void registerModelLoader(RegisterGeometryLoaders event) {
        event.register("table", TableModel.LOADER);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityRendererFactory<TableBlockEntity> tableRenderer = TableTileEntityRenderer::new;
        event.registerBlockEntityRenderer(TinkerTables.craftingStationTile.get(), tableRenderer);
        event.registerBlockEntityRenderer(TinkerTables.tinkerStationTile.get(), tableRenderer);
        event.registerBlockEntityRenderer(TinkerTables.modifierWorktableTile.get(), tableRenderer);
        event.registerBlockEntityRenderer(TinkerTables.partBuilderTile.get(), tableRenderer);
    }

    @SubscribeEvent
    static void setupClient(final FMLClientSetupEvent event) {
        HandledScreens.register(TinkerTables.craftingStationContainer.get(), CraftingStationScreen::new);
        HandledScreens.register(TinkerTables.tinkerStationContainer.get(), TinkerStationScreen::new);
        HandledScreens.register(TinkerTables.partBuilderContainer.get(), PartBuilderScreen::new);
        HandledScreens.register(TinkerTables.modifierWorktableContainer.get(), ModifierWorktableScreen::new);
        HandledScreens.register(TinkerTables.tinkerChestContainer.get(), TinkerChestScreen::new);
    }

    @SubscribeEvent
    static void registerBlockColors(final RegisterColorHandlersEvent.Block event) {
        event.getBlockColors().register((state, world, pos, index) -> {
            if (world != null && pos != null) {
                BlockEntity te = world.getBlockEntity(pos);
                if (te instanceof TinkersChestBlockEntity) {
                    return ((TinkersChestBlockEntity) te).getColor();
                }
            }
            return -1;
        }, TinkerTables.tinkersChest.get());
    }

    @SubscribeEvent
    static void registerItemColors(final RegisterColorHandlersEvent.Item event) {
        event.register((stack, index) -> ((DyeableLeatherItem) stack.getItem()).getColor(stack), TinkerTables.tinkersChest.asItem());
    }
}
