package slimeknights.tconstruct.plugin.jsonthings.item;

import dev.gigaherz.jsonthings.things.IFlexItem;
import dev.gigaherz.jsonthings.things.StackContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import lombok.Getter;
import net.minecraft.item.ItemGroup;
import slimeknights.tconstruct.tools.item.RepairKitItem;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Item for custom repair kits
 */
public class FlexRepairKitItem extends RepairKitItem implements IFlexItem {
    private final Map<String, FlexEventHandler> eventHandlers = new HashMap<>();
    private final Set<ItemGroup> tabs = new HashSet<>();

    @Getter
    private final float repairAmount;

    public FlexRepairKitItem(Settings properties, float repairAmount) {
        super(properties);
        this.repairAmount = repairAmount;
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
