package slimeknights.tconstruct.gadgets.item;

import slimeknights.mantle.util.TranslationHelper;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.gadgets.entity.shuriken.ShurikenEntityBase;

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
import java.util.function.BiFunction;

// TODO: lot of code here looks like glow ball and efln, shared base class?
public class ShurikenItem extends SnowballItem {

    private final BiFunction<World, PlayerEntity, ShurikenEntityBase> entity;

    public ShurikenItem(Settings properties, BiFunction<World, PlayerEntity, ShurikenEntityBase> entity) {
        super(properties);
        this.entity = entity;
    }

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Sounds.SHURIKEN_THROW.getSound(), SoundCategory.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        player.getItemCooldownManager().set(stack.getItem(), 4);
        if (!level.isClient()) {
            ShurikenEntityBase entity = this.entity.apply(level, player);
            entity.setItem(stack);
            entity.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, 1.5F, 1.0F);
            level.spawnEntity(entity);
        }
        player.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }

        return TypedActionResult.success(stack, level.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltip, TooltipContext flag) {
        TranslationHelper.addOptionalTooltip(stack, tooltip);
        super.appendTooltip(stack, level, tooltip, flag);
    }
}
