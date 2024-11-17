package slimeknights.tconstruct.plugin.jsonthings.item.armor;

import dev.gigaherz.jsonthings.things.IFlexItem;
import dev.gigaherz.jsonthings.things.StackContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;

import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Common code for a modifiable armor item in JSON Things
 */
public class FlexModifiableArmorItem extends ModifiableArmorItem implements IFlexItem {
    private final Map<String, FlexEventHandler> eventHandlers = new HashMap<>();
    private final Set<ItemGroup> tabs = new HashSet<>();

    public FlexModifiableArmorItem(ArmorMaterial materialIn, EquipmentSlot slot, Settings builderIn, ToolDefinition toolDefinition) {
        super(materialIn, slot, builderIn, toolDefinition);
    }


    /* JSON things does not use the item properties tab, they handle it via the below method */

    @Override
    public void addCreativeStack(StackContext stackContext, Iterable<ItemGroup> tabs) {
        for (ItemGroup tab : tabs) {
            this.tabs.add(tab);
        }
    }

    @Override
    protected boolean allowedIn(ItemGroup category) {
        return this.tabs.contains(category);
    }


    /* not honestly sure what events do, but trivial to support */

    @Override
    public void addEventHandler(String name, FlexEventHandler flexEventHandler) {
        this.eventHandlers.put(name, flexEventHandler);
    }

    @Nullable
    @Override
    public FlexEventHandler getEventHandler(String name) {
        return this.eventHandlers.get(name);
    }
}
