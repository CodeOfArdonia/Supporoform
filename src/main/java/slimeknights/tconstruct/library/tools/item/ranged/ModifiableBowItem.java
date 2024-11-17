package slimeknights.tconstruct.library.tools.item.ranged;

import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.ForgeEventFactory;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.BowAmmoModifierHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.modifiers.ability.interaction.BlockingModifier;
import slimeknights.tconstruct.tools.modifiers.upgrades.ranged.ScopeModifier;

import java.util.function.Predicate;

import static slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook.KEY_DRAWTIME;

public class ModifiableBowItem extends ModifiableLauncherItem {
    public ModifiableBowItem(Settings properties, ToolDefinition toolDefinition) {
        super(properties, toolDefinition);
    }


    /* Properties */

    @Override
    public Predicate<ItemStack> getProjectiles() {
        return RangedWeaponItem.BOW_PROJECTILES;
    }

    @Override
    public int getRange() {
        return 15;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return BlockingModifier.blockWhileCharging(ToolStack.from(stack), UseAction.BOW);
    }


    /* Arrow launching */

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack bow = player.getStackInHand(hand);
        ToolStack tool = ToolStack.from(bow);
        if (tool.isBroken()) {
            return TypedActionResult.fail(bow);
        }

        boolean hasAmmo = BowAmmoModifierHook.hasAmmo(tool, bow, player, getHeldProjectiles());
        // ask forge if it has any different opinions
        TypedActionResult<ItemStack> override = ForgeEventFactory.onArrowNock(bow, level, player, hand, hasAmmo);
        if (override != null) {
            return override;
        }
        // if no ammo, cannot fire
        if (!player.getAbilities().creativeMode && !hasAmmo) {
            // however, we can block if enabled
            if (ModifierUtil.canPerformAction(tool, ToolActions.SHIELD_BLOCK)) {
                player.setCurrentHand(hand);
                return TypedActionResult.consume(bow);
            }
            return TypedActionResult.fail(bow);
        }
        GeneralInteractionModifierHook.startDrawtime(tool, player, 1);
        player.setCurrentHand(hand);
        if (!level.isClient) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), Sounds.LONGBOW_CHARGE.getSound(), SoundCategory.PLAYERS, 0.75F, 1.0F);
        }
        return TypedActionResult.consume(bow);
    }

    @Override
    public void onStoppedUsing(ItemStack bow, World level, LivingEntity living, int timeLeft) {
        // clear zoom regardless, does not matter if the tool broke, we should not be zooming
        ScopeModifier.stopScoping(living);

        // need player
        if (!(living instanceof PlayerEntity player)) {
            return;
        }
        // no broken
        ToolStack tool = ToolStack.from(bow);
        if (tool.isBroken()) {
            tool.getPersistentData().remove(KEY_DRAWTIME);
            return;
        }

        // just not handling vanilla infinity at all, we have our own hooks which someone could use to mimic infinity if they wish with a bit of effort
        boolean creative = player.getAbilities().creativeMode;
        // its a little redundant to search for ammo twice, but otherwise we risk shrinking the stack before we know if we can fire
        // sldo helps blocking, as you can block without ammo
        boolean hasAmmo = creative || BowAmmoModifierHook.hasAmmo(tool, bow, player, getHeldProjectiles());

        // ask forge its thoughts on shooting
        int chargeTime = this.getMaxUseTime(bow) - timeLeft;
        chargeTime = ForgeEventFactory.onArrowLoose(bow, level, player, chargeTime, hasAmmo);

        // no ammo? no charge? nothing to do
        if (!hasAmmo || chargeTime < 0) {
            tool.getPersistentData().remove(KEY_DRAWTIME);
            return;
        }

        // calculate arrow power
        float charge = GeneralInteractionModifierHook.getToolCharge(tool, chargeTime);
        tool.getPersistentData().remove(KEY_DRAWTIME);
        float velocity = ConditionalStatModifierHook.getModifiedStat(tool, living, ToolStats.VELOCITY);
        float power = charge * velocity;
        if (power < 0.1f) {
            return;
        }

        // launch the arrow
        if (!level.isClient) {
            // find ammo after the return above, as otherwise we might consume ammo before
            ItemStack ammo = BowAmmoModifierHook.findAmmo(tool, bow, player, getHeldProjectiles());
            // could only be empty at this point if we are creative, as hasAmmo returned true above
            if (ammo.isEmpty()) {
                ammo = new ItemStack(Items.ARROW);
            }

            // prepare the arrows
            ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem arrow ? arrow : (ArrowItem) Items.ARROW;
            float inaccuracy = ModifierUtil.getInaccuracy(tool, living);
            float startAngle = getAngleStart(ammo.getCount());
            int primaryIndex = ammo.getCount() / 2;
            for (int arrowIndex = 0; arrowIndex < ammo.getCount(); arrowIndex++) {
                PersistentProjectileEntity arrow = arrowItem.createArrow(level, ammo, player);
                float angle = startAngle + (10 * arrowIndex);
                arrow.setVelocity(player, player.getPitch() + angle, player.getYaw(), 0, power * 3.0F, inaccuracy);
                if (charge == 1.0F) {
                    arrow.setCritical(true);
                }

                // vanilla arrows have a base damage of 2, cancel that out then add in our base damage to account for custom arrows with higher base damage
                // calculate it just once as all four arrows are the same item, they should have the same damage
                float baseArrowDamage = (float) (arrow.getDamage() - 2 + tool.getStats().get(ToolStats.PROJECTILE_DAMAGE));
                arrow.setDamage(ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.PROJECTILE_DAMAGE, baseArrowDamage));

                // just store all modifiers on the tool for simplicity
                ModifierNBT modifiers = tool.getModifiers();
                arrow.getCapability(EntityModifierCapability.CAPABILITY).ifPresent(cap -> cap.setModifiers(modifiers));

                // fetch the persistent data for the arrow as modifiers may want to store data
                NamespacedNBT arrowData = PersistentDataCapability.getOrWarn(arrow);

                // if infinite, skip pickup
                if (creative) {
                    arrow.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                }

                // let modifiers such as fiery and punch set properties
                for (ModifierEntry entry : modifiers.getModifiers()) {
                    entry.getHook(ModifierHooks.PROJECTILE_LAUNCH).onProjectileLaunch(tool, entry, living, arrow, arrow, arrowData, arrowIndex == primaryIndex);
                }
                level.spawnEntity(arrow);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + charge * 0.5F + (angle / 10f));
            }
            ToolDamageUtil.damageAnimated(tool, ammo.getCount(), player, player.getActiveHand());
        }

        // stats and sounds
        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }
}
