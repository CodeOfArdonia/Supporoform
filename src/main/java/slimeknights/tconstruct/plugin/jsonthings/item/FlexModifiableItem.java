package slimeknights.tconstruct.plugin.jsonthings.item;

import dev.gigaherz.jsonthings.things.IFlexItem;
import dev.gigaherz.jsonthings.things.StackContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FlexModifiableItem extends ModifiableItem implements IFlexItem {
    private final Map<String, FlexEventHandler> eventHandlers = new HashMap<>();
    private final Set<ItemGroup> tabs = new HashSet<>();
    private final boolean breakBlocksInCreative;

    public FlexModifiableItem(Settings properties, ToolDefinition toolDefinition, boolean breakBlocksInCreative) {
        super(properties, toolDefinition);
        this.breakBlocksInCreative = breakBlocksInCreative;
    }

    @Override
    public boolean canMine(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity player) {
        return this.breakBlocksInCreative || !player.isCreative();
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
