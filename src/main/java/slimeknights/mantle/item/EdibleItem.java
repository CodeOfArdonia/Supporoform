package slimeknights.mantle.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.TranslationHelper;

import java.util.List;
import java.util.Objects;

public class EdibleItem extends Item {
    public EdibleItem(FoodComponent foodIn) {
        this(new Settings().food(foodIn));
    }

    public EdibleItem(Settings properties) {
        super(properties);
        Objects.requireNonNull(this.getFoodComponent(), "Must set food to make an EdibleItem");
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        TranslationHelper.addOptionalTooltip(stack, tooltip);
        for (Pair<StatusEffectInstance, Float> pair : Objects.requireNonNull(stack.getItem().getFoodComponent(stack, null)).getEffects()) {
            if (pair.getFirst() != null) {
                tooltip.add(Text.literal(I18n.translate(pair.getFirst().getTranslationKey()).trim()).formatted(Formatting.GRAY));
            }
        }
    }
}
