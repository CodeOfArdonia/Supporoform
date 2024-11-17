package slimeknights.mantle.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.TranslationHelper;

import java.util.List;

public class ArmorTooltipItem extends ArmorItem {

    public ArmorTooltipItem(ArmorMaterial armorMaterial, Type type, Settings builder) {
        super(armorMaterial, type, builder);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        TranslationHelper.addOptionalTooltip(stack, tooltip);
        super.appendTooltip(stack, worldIn, tooltip, flagIn);
    }
}
