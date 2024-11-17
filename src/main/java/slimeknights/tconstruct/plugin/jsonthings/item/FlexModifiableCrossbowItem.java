package slimeknights.tconstruct.plugin.jsonthings.item;

import dev.gigaherz.jsonthings.things.IFlexItem;
import dev.gigaherz.jsonthings.things.StackContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;

import org.jetbrains.annotations.Nullable;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class FlexModifiableCrossbowItem extends ModifiableCrossbowItem implements IFlexItem {
    private final Map<String, FlexEventHandler> eventHandlers = new HashMap<>();
    private final Set<ItemGroup> tabs = new HashSet<>();

    private final boolean allowFireworks;

    public FlexModifiableCrossbowItem(Settings properties, ToolDefinition toolDefinition, boolean allowFireworks) {
        super(properties, toolDefinition);
        this.allowFireworks = allowFireworks;
    }

    @Override
    public Predicate<ItemStack> getHeldProjectiles() {
        return this.allowFireworks ? CROSSBOW_HELD_PROJECTILES : BOW_PROJECTILES;
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
