package slimeknights.tconstruct.smeltery.menu;

import lombok.Getter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.Property;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import slimeknights.mantle.inventory.SmartItemHandlerSlot;
import slimeknights.mantle.util.sync.ValidZeroDataSlot;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.inventory.TriggeringBaseContainerMenu;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.MelterBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MelterContainerMenu extends TriggeringBaseContainerMenu<MelterBlockEntity> {
    public static final Identifier TOOLTIP_FORMAT = TConstruct.getResource("melter");

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    @Getter
    private final Slot[] inputs;
    @Getter
    private boolean hasFuelSlot = false;

    public MelterContainerMenu(int id, @Nullable PlayerInventory inv, @Nullable MelterBlockEntity melter) {
        super(TinkerSmeltery.melterContainer.get(), id, inv, melter);

        // create slots
        if (melter != null) {
            MeltingModuleInventory inventory = melter.getMeltingInventory();
            this.inputs = new Slot[inventory.getSlots()];
            for (int i = 0; i < this.inputs.length; i++) {
                this.inputs[i] = this.addSlot(new SmartItemHandlerSlot(inventory, i, 22, 16 + (i * 18)));
            }

            // add fuel slot if present, we only add for the melter though
            World world = melter.getWorld();
            BlockPos down = melter.getPos().down();
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
            ValidZeroDataSlot.trackIntArray(referenceConsumer, melter.getFuelModule());
            inventory.trackInts(array -> ValidZeroDataSlot.trackIntArray(referenceConsumer, array));
        } else {
            this.inputs = new Slot[0];
        }
    }

    public MelterContainerMenu(int id, PlayerInventory inv, PacketByteBuf buf) {
        this(id, inv, getTileEntityFromBuf(buf, MelterBlockEntity.class));
    }
}
