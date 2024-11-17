package slimeknights.tconstruct.fluids.item;

import com.mojang.datafixers.util.Pair;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.fluids.util.ConstantFluidContainerWrapper;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ContainerFoodItem extends Item {
    public ContainerFoodItem(Settings props) {
        super(props);
    }

    @Override
    public int getMaxUseTime(ItemStack pStack) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack pStack) {
        return UseAction.DRINK;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext flagIn) {
        // add effects to the tooltip, code based on potion items
        for (Pair<StatusEffectInstance, Float> pair : Objects.requireNonNull(stack.getItem().getFoodComponent()).getStatusEffects()) {
            StatusEffectInstance effect = pair.getFirst();
            if (effect != null) {
                MutableText mutable = Text.translatable(effect.getTranslationKey());
                if (effect.getAmplifier() > 0) {
                    mutable = Text.translatable("potion.withAmplifier", mutable, Text.translatable("potion.potency." + effect.getAmplifier()));
                }
                if (effect.getDuration() > 20) {
                    mutable = Text.translatable("potion.withDuration", mutable, StatusEffectUtil.getDurationText(effect, 1.0f));
                }
                tooltip.add(mutable.formatted(effect.getEffectType().getCategory().getFormatting()));
            }
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World level, LivingEntity living) {
        ItemStack container = stack.getRecipeRemainder();
        ItemStack result = super.finishUsing(stack, level, living);
        PlayerEntity player = living instanceof PlayerEntity p ? p : null;
        if (player == null || !player.getAbilities().creativeMode) {
            if (result.isEmpty()) {
                return container.copy();
            }
            if (player != null) {
                if (!player.getInventory().insertStack(container.copy())) {
                    player.dropItem(stack, false);
                }
            }
        }
        return result;
    }

    public static class FluidContainerFoodItem extends ContainerFoodItem {
        private final Supplier<FluidStack> fluid;

        public FluidContainerFoodItem(Settings props, Supplier<FluidStack> fluid) {
            super(props);
            this.fluid = fluid;
        }

        @Nullable
        @Override
        public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NbtCompound nbt) {
            return new ConstantFluidContainerWrapper(fluid.get(), stack);
        }
    }
}
