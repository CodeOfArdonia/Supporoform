package slimeknights.tconstruct.tools.modifiers.slotless;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.ArrayList;
import java.util.List;

/**
 * Modifier that adds a variable number of slots to a tool. Could easily be done via Tag editing, but this makes it easier
 */
public class CreativeSlotModifier extends NoLevelsModifier implements VolatileDataModifierHook, ModifierRemovalHook {
    /**
     * Key representing the slots object in the modifier
     */
    public static final Identifier KEY_SLOTS = TConstruct.getResource("creative");

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.VOLATILE_DATA, ModifierHooks.REMOVE);
    }

    @Override
    public Text onRemoved(IToolStackView tool, Modifier modifier) {
        tool.getPersistentData().remove(KEY_SLOTS);
        return null;
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ModDataNBT volatileData) {
        IModDataView persistentData = context.getPersistentData();
        if (persistentData.contains(KEY_SLOTS, NbtElement.COMPOUND_TYPE)) {
            NbtCompound slots = persistentData.getCompound(KEY_SLOTS);
            for (String key : slots.getKeys()) {
                SlotType slotType = SlotType.getIfPresent(key);
                if (slotType != null) {
                    volatileData.addSlots(slotType, slots.getInt(key));
                }
            }
        }
    }

    /**
     * Formats the given slot type as a count
     */
    private static Text formatCount(SlotType slotType, int count) {
        return Text.literal((count > 0 ? "+" : "") + count + " ")
                .append(slotType.getDisplayName())
                .styled(style -> style.withColor(slotType.getColor()));
    }

    @Override
    public List<Text> getDescriptionList(IToolStackView tool, ModifierEntry entry) {
        List<Text> tooltip = this.getDescriptionList(entry.getLevel());
        IModDataView persistentData = tool.getPersistentData();
        if (persistentData.contains(KEY_SLOTS, NbtElement.COMPOUND_TYPE)) {
            NbtCompound slots = persistentData.getCompound(KEY_SLOTS);

            // first one found has special behavior
            boolean first = true;
            for (String key : slots.getKeys()) {
                SlotType slotType = SlotType.getIfPresent(key);
                if (slotType != null) {
                    if (first) {
                        // found a valid slot? copy the list once then add the rest
                        tooltip = new ArrayList<>(tooltip);
                        first = false;
                    }
                    tooltip.add(formatCount(slotType, slots.getInt(key)));
                }
            }
        }
        return tooltip;
    }
}
