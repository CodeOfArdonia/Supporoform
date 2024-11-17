package slimeknights.tconstruct.tools.logic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.events.ToolEquipmentChangeEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

/**
 * Capability to make it easy for modifiers to store common data on the player, primarily used for armor
 */
public class EquipmentChangeWatcher {
    private EquipmentChangeWatcher() {
    }

    /**
     * Capability ID
     */
    private static final Identifier ID = TConstruct.getResource("equipment_watcher");
    /**
     * Capability type
     */
    public static final Capability<PlayerLastEquipment> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    /**
     * Registers this capability
     */
    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class, event -> event.register(PlayerLastEquipment.class));

        // equipment change is used on both sides
        MinecraftForge.EVENT_BUS.addListener(EquipmentChangeWatcher::onEquipmentChange);

        // only need to use the cap and the player tick on the client
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.addListener(EquipmentChangeWatcher::onPlayerTick);
            MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, EquipmentChangeWatcher::attachCapability);
        }
    }


    /* Events */

    /**
     * Serverside modifier hooks
     */
    private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        runModifierHooks(event.getEntity(), event.getSlot(), event.getFrom(), event.getTo());
    }

    /**
     * Event listener to attach the capability
     */
    private static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity.getEntityWorld().isClient && entity instanceof PlayerEntity) {
            PlayerLastEquipment provider = new PlayerLastEquipment((PlayerEntity) entity);
            event.addCapability(ID, provider);
            event.addListener(provider);
        }
    }

    /**
     * Client side modifier hooks
     */
    private static void onPlayerTick(PlayerTickEvent event) {
        // only run for client side players every 5 ticks
        if (event.phase == Phase.END && event.side == LogicalSide.CLIENT) {
            event.player.getCapability(CAPABILITY).ifPresent(PlayerLastEquipment::update);
        }
    }


    /* Helpers */

    /**
     * Shared modifier hook logic
     */
    private static void runModifierHooks(LivingEntity entity, EquipmentSlot changedSlot, ItemStack original, ItemStack replacement) {
        EquipmentChangeContext context = new EquipmentChangeContext(entity, changedSlot, original, replacement);

        // first, fire event to notify an item was removed
        IToolStackView tool = context.getOriginalTool();
        if (tool != null) {
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.EQUIPMENT_CHANGE).onUnequip(tool, entry, context);
            }
            // only path that should bring you here that did not already call the modifier method is when your shield breaks. ideally we will switch to a forge onStoppedUsing method instead
            // TODO 1.19: consider simplier check, such as the tool having the active modifier tag set. Will need to do a bit of work for bows which don't set modifiers though
            if (!entity.isUsingItem() || entity.getEquippedStack(changedSlot) != entity.getActiveItem()) {
                GeneralInteractionModifierHook.finishUsing(tool);
            }
        }

        // next, fire event to notify an item was added
        tool = context.getReplacementTool();
        if (tool != null) {
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.EQUIPMENT_CHANGE).onEquip(tool, entry, context);
            }
        }

        // finally, fire events on all other slots to say something changed
        for (EquipmentSlot otherSlot : EquipmentSlot.values()) {
            if (otherSlot != changedSlot) {
                tool = context.getToolInSlot(otherSlot);
                if (tool != null) {
                    for (ModifierEntry entry : tool.getModifierList()) {
                        entry.getHook(ModifierHooks.EQUIPMENT_CHANGE).onEquipmentChange(tool, entry, context, otherSlot);
                    }
                }
            }
        }
        // fire event for modifiers that want to watch equipment when not equipped
        MinecraftForge.EVENT_BUS.post(new ToolEquipmentChangeEvent(context));
    }

    /* Required methods */

    /**
     * Data class that runs actual update logic
     */
    protected static class PlayerLastEquipment implements ICapabilityProvider, Runnable {
        @Nullable
        private final PlayerEntity player;
        private final Map<EquipmentSlot, ItemStack> lastItems = new EnumMap<>(EquipmentSlot.class);
        private LazyOptional<PlayerLastEquipment> capability;

        private PlayerLastEquipment(@Nullable PlayerEntity player) {
            this.player = player;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                this.lastItems.put(slot, ItemStack.EMPTY);
            }
            this.capability = LazyOptional.of(() -> this);
        }

        /**
         * Called on player tick to update the stacks and run the event
         */
        public void update() {
            // run twice a second, should be plenty fast enough
            if (this.player != null) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack newStack = this.player.getEquippedStack(slot);
                    ItemStack oldStack = this.lastItems.get(slot);
                    if (!ItemStack.areEqual(oldStack, newStack)) {
                        this.lastItems.put(slot, newStack.copy());
                        runModifierHooks(this.player, slot, oldStack, newStack);
                    }
                }
            }
        }

        /**
         * Called on capability invalidate to invalidate
         */
        @Override
        public void run() {
            this.capability.invalidate();
            this.capability = LazyOptional.of(() -> this);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, this.capability);
        }
    }
}
