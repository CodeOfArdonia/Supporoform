package slimeknights.tconstruct.gadgets.item;

import slimeknights.mantle.util.TranslationHelper;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.gadgets.entity.GlowballEntity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SnowballItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class GlowBallItem extends SnowballItem {

    public GlowBallItem() {
        super((new Settings()).maxCount(16).tab(TinkerGadgets.TAB_GADGETS));
    }

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getStackInHand(handIn);
        if (!playerIn.getAbilities().creativeMode) {
            itemstack.decrement(1);
        }

        level.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), Sounds.THROWBALL_THROW.getSound(), SoundCategory.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClient) {
            GlowballEntity glowballEntity = new GlowballEntity(level, playerIn);
            glowballEntity.setItem(itemstack);
            glowballEntity.setVelocity(playerIn, playerIn.getPitch(), playerIn.getYaw(), 0.0F, 1.5F, 1.0F);
            level.spawnEntity(glowballEntity);
        }

        playerIn.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(itemstack, level.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext flag) {
        TranslationHelper.addOptionalTooltip(stack, tooltip);
        super.appendTooltip(stack, level, tooltip, flag);
    }
}
