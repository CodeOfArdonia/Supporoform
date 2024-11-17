package slimeknights.tconstruct.shared.item;

import net.minecraft.item.Items;
import slimeknights.tconstruct.TConstruct;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;

public class CheeseItem extends Item {
    public static final Text TOOLTIP = TConstruct.makeTranslation("item", "cheese.tooltip");

    public CheeseItem(Settings pProperties) {
        super(pProperties);
    }

    /**
     * Removes a random effect from the given entity
     */
    public static void removeRandomEffect(LivingEntity living) {
        if (!living.getWorld().isClient) {
            Collection<StatusEffectInstance> effects = living.getStatusEffects();
            if (!effects.isEmpty()) {
                // don't remove effects that are not milk removable
                List<StatusEffect> removable = effects.stream().filter(effect -> effect.getCurativeItems().stream().anyMatch(item -> item.isOf(Items.MILK_BUCKET))).map(StatusEffectInstance::getEffectType).toList();
                if (!removable.isEmpty()) {
                    living.removeStatusEffect(removable.get(living.getRandom().nextInt(removable.size())));
                }
            }
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World level, LivingEntity living) {
        removeRandomEffect(living);
        return super.finishUsing(stack, level, living);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World pLevel, List<Text> tooltip, TooltipContext pIsAdvanced) {
        tooltip.add(TOOLTIP);
    }
}
