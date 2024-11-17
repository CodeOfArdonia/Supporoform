package slimeknights.tconstruct.library.tools.part;

import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;

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
 * Item which shows the cast cost in the tooltip
 */
public class PartCastItem extends Item {
    public static final String COST_KEY = TConstruct.makeTranslationKey("item", "cast.cost");

    private final Supplier<? extends IMaterialItem> part;

    public PartCastItem(Settings props, Supplier<? extends IMaterialItem> part) {
        super(props);
        this.part = part;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext flag) {
        int cost = MaterialCastingLookup.getItemCost(this.part.get());
        if (cost > 0) {
            tooltip.add(Text.translatable(COST_KEY, cost).formatted(Formatting.GRAY));
        }
    }
}
