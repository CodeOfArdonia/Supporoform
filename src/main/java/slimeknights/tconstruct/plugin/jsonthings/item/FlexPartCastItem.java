package slimeknights.tconstruct.plugin.jsonthings.item;

import dev.gigaherz.jsonthings.things.builders.ItemBuilder;
import dev.gigaherz.jsonthings.things.items.FlexItem;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.tools.part.PartCastItem;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Supplier;

/**
 * Tool part cast item, which adds the part cost to the tooltip
 */
public class FlexPartCastItem extends FlexItem {
    private final Supplier<Item> part;

    public FlexPartCastItem(Properties properties, ItemBuilder builder, Supplier<Item> part) {
        super(properties, builder);
        this.part = part;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext flag) {
        int cost = MaterialCastingLookup.getItemCost(this.part.get());
        if (cost > 0) {
            tooltip.add(Text.translatable(PartCastItem.COST_KEY, cost).formatted(Formatting.GRAY));
        }
    }
}
