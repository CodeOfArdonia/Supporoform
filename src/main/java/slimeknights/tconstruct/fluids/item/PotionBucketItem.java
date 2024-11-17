package slimeknights.tconstruct.fluids.item;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.item.FluidBucketWrapper;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;
import java.util.function.Supplier;

/**
 * Implements filling a bucket with an NBT fluid
 */
public class PotionBucketItem extends PotionItem {
    private final Supplier<? extends Fluid> supplier;

    public PotionBucketItem(Supplier<? extends Fluid> supplier, Settings builder) {
        super(builder);
        this.supplier = supplier;
    }

    public Fluid getFluid() {
        return supplier.get();
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        String bucketKey = PotionUtil.getPotion(stack.getNbt()).finishTranslationKey(getTranslationKey() + ".effect.");
        if (Util.canTranslate(bucketKey)) {
            return bucketKey;
        }
        return super.getTranslationKey();
    }

    @Override
    public Text getName(ItemStack stack) {
        Potion potion = PotionUtil.getPotion(stack.getNbt());
        String bucketKey = potion.finishTranslationKey(getTranslationKey() + ".effect.");
        if (Util.canTranslate(bucketKey)) {
            return Text.translatable(bucketKey);
        }
        // default to filling with the contents
        return Text.translatable(getTranslationKey() + ".contents", Text.translatable(potion.finishTranslationKey("item.minecraft.potion.effect.")));
    }

    @Override
    public ItemStack getDefaultStack() {
        return PotionUtil.setPotion(super.getDefaultStack(), Potions.WATER);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World level, LivingEntity living) {
        PlayerEntity player = living instanceof PlayerEntity p ? p : null;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Criteria.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        // effects are 2x duration
        if (!level.isClient) {
            for (StatusEffectInstance effect : PotionUtil.getPotionEffects(stack)) {
                if (effect.getEffectType().isInstant()) {
                    effect.getEffectType().applyInstantEffect(player, player, living, effect.getAmplifier(), 2.5D);
                } else {
                    StatusEffectInstance newEffect = new StatusEffectInstance(effect);
                    newEffect.duration = newEffect.duration * 5 / 2;
                    living.addStatusEffect(newEffect);
                }
            }
        }

        if (player != null) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }

        if (player == null || !player.getAbilities().creativeMode) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.BUCKET);
            }
            if (player != null) {
                player.getInventory().insertStack(new ItemStack(Items.BUCKET));
            }
        }
        living.emitGameEvent(GameEvent.DRINK);
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack pStack, @Nullable World pLevel, List<Text> pTooltip, TooltipContext pFlag) {
        PotionUtil.buildTooltip(pStack, pTooltip, 2.5f);
    }

    @Override
    public int getMaxUseTime(ItemStack pStack) {
        return 96; // 3x duration of potion bottles
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NbtCompound nbt) {
        return new PotionBucketWrapper(stack);
    }

    public static class PotionBucketWrapper extends FluidBucketWrapper {
        public PotionBucketWrapper(ItemStack container) {
            super(ContainerItemContext.withConstant(container));
        }

        @NotNull
        @Override
        public FluidStack getFluid() {
            return new FluidStack(((PotionBucketItem) container.getItem()).getFluid(),
                    FluidType.BUCKET_VOLUME, container.getTag());
        }
    }
}
