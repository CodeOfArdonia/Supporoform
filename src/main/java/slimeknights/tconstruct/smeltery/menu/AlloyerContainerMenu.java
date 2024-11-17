package slimeknights.tconstruct.smeltery.menu;

import lombok.Getter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.mantle.util.sync.ValidZeroDataSlot;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.inventory.TriggeringBaseContainerMenu;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.AlloyerBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.alloying.MixerAlloyTank;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class AlloyerContainerMenu extends TriggeringBaseContainerMenu<AlloyerBlockEntity> {
    public static final Identifier TOOLTIP_FORMAT = TConstruct.getResource("alloyer");

    @Getter
    private boolean hasFuelSlot = false;

    public AlloyerContainerMenu(int id, @Nullable PlayerInventory inv, @Nullable AlloyerBlockEntity alloyer) {
        super(TinkerSmeltery.alloyerContainer.get(), id, inv, alloyer);

        // create slots
        if (alloyer != null) {
            // refresh cache of neighboring tanks
            World world = alloyer.getWorld();
            if (world != null && world.isClient) {
                MixerAlloyTank alloyTank = alloyer.getAlloyTank();
                for (Direction direction : Direction.values()) {
                    if (direction != Direction.DOWN) {
                        alloyTank.refresh(direction, true);
                    }
                }
            }

            // add fuel slot if present
            BlockPos down = alloyer.getPos().down();
            if (world != null && world.getBlockState(down).isIn(TinkerTags.Blocks.FUEL_TANKS)) {
                BlockEntity te = world.getBlockEntity(down);
                if (te != null) {
                    this.hasFuelSlot = te.getCapability(ForgeCapabilities.ITEM_HANDLER).filter(handler -> {
                        this.addSlot(new SmartItemHandlerSlot(handler, 0, 151, 32));
                        return true;
                    }).isPresent();
                }
            }

            this.addInventorySlots();

            // syncing
            Consumer<Property> referenceConsumer = this::addProperty;
            ValidZeroDataSlot.trackIntArray(referenceConsumer, alloyer.getFuelModule());
        }
    }

    public AlloyerContainerMenu(int id, PlayerInventory inv, PacketByteBuf buf) {
        this(id, inv, getTileEntityFromBuf(buf, AlloyerBlockEntity.class));
    }
}
