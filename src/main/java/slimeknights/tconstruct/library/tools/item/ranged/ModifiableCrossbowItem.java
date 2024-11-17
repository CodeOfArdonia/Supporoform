package slimeknights.tconstruct.library.tools.item.ranged;

import net.minecraft.util.math.Vec3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolActions;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
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
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.ability.interaction.BlockingModifier;
import slimeknights.tconstruct.tools.modifiers.upgrades.ranged.ScopeModifier;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook.KEY_DRAWTIME;

public class ModifiableCrossbowItem extends ModifiableLauncherItem {
    /**
     * Key containing the stored crossbow ammo
     */
    public static final Identifier KEY_CROSSBOW_AMMO = TConstruct.getResource("crossbow_ammo");
    private static final String PROJECTILE_KEY = "item.minecraft.crossbow.projectile";

    public ModifiableCrossbowItem(Settings properties, ToolDefinition toolDefinition) {
        super(properties, toolDefinition);
    }


    /* Properties */

    @Override
    public Predicate<ItemStack> getHeldProjectiles() {
        return CROSSBOW_HELD_PROJECTILES;
    }

    @Override
    public Predicate<ItemStack> getProjectiles() {
        return BOW_PROJECTILES;
    }

    @Override
    public int getRange() {
        return 8;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        // crossbow is superhardcoded to crossbows, so use none and rely on the model
        return BlockingModifier.blockWhileCharging(ToolStack.from(stack), UseAction.NONE);
    }

    @Override
    public boolean isUsedOnRelease(ItemStack stack) {
        return true;
    }


    /* Arrow launching */

    /**
     * Gets the arrow pitch
     */
    private static float getRandomShotPitch(float angle, Random pRandom) {
        if (angle == 0) {
            return 1.0f;
        }
        return 1.0F / (pRandom.nextFloat() * 0.5F + 1.8F) + 0.53f + (angle / 10f);
    }

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack bow = player.getStackInHand(hand);

        ToolStack tool = ToolStack.from(bow);
        if (tool.isBroken()) {
            return TypedActionResult.fail(bow);
        }

        // yeah, its hardcoded, I cannot see a need to not hardcode this, request it if you need it
        boolean sinistral = hand == Hand.MAIN_HAND && tool.getModifierLevel(TinkerModifiers.sinistral.getId()) > 0;

        // no ammo? not charged
        ModDataNBT persistentData = tool.getPersistentData();
        NbtCompound heldAmmo = persistentData.getCompound(KEY_CROSSBOW_AMMO);
        if (heldAmmo.isEmpty()) {
            // do not charge if sneaking and we have sinistral, gives you a way to activate the offhand when the crossbow is not charged
            if (sinistral && !player.getOffHandStack().isEmpty() && player.isInSneakingPose()) {
                return TypedActionResult.pass(bow);
            }

            // if we have ammo, start charging
            if (BowAmmoModifierHook.hasAmmo(tool, bow, player, getHeldProjectiles())) {
                GeneralInteractionModifierHook.startDrawtime(tool, player, 1);
                player.setCurrentHand(hand);
                if (!level.isClient) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_CROSSBOW_QUICK_CHARGE_1, SoundCategory.PLAYERS, 0.75F, 1.0F);
                }
                return TypedActionResult.consume(bow);
                // no ammo still lets us block
            } else if (ModifierUtil.canPerformAction(tool, ToolActions.SHIELD_BLOCK)) {
                player.setCurrentHand(hand);
                return TypedActionResult.consume(bow);
            } else {
                return TypedActionResult.fail(bow);
            }
        }

        // sinistral shoots on left click when in main hand, and lets us block instead of shooting if the offhand is empty
        if (sinistral) {
            ItemStack offhand = player.getOffHandStack();
            if (!offhand.isEmpty() && !offhand.isOf(Items.FIREWORK_ROCKET)) {
                return TypedActionResult.pass(bow);
            }
            if (ModifierUtil.canPerformAction(tool, ToolActions.SHIELD_BLOCK)) {
                player.setCurrentHand(hand);
                return TypedActionResult.consume(bow);
            }
        }

        // ammo already loaded? time to fire
        fireCrossbow(tool, player, hand, heldAmmo);
        return TypedActionResult.consume(bow);
    }

    /**
     * Fires the crossbow
     *
     * @param tool     Tool instance
     * @param player   Player firing
     * @param hand     Hand fired from
     * @param heldAmmo Ammo used to fire, should be non-empty
     */
    public static void fireCrossbow(IToolStackView tool, PlayerEntity player, Hand hand, NbtCompound heldAmmo) {
        // ammo already loaded? time to fire
        World level = player.getWorld();
        if (!level.isClient) {
            // shoot the projectile
            int damage = 0;

            // don't need to calculate these multiple times
            float velocity = ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.VELOCITY);
            float inaccuracy = ModifierUtil.getInaccuracy(tool, player);
            boolean creative = player.getAbilities().creativeMode;

            // the ammo has a stack size that may be greater than 1 (meaning multishot)
            // when creating the ammo stacks, we use split, so its getting smaller each time
            ItemStack ammo = ItemStack.fromNbt(heldAmmo);
            float startAngle = getAngleStart(ammo.getCount());
            int primaryIndex = ammo.getCount() / 2;
            for (int arrowIndex = 0; arrowIndex < ammo.getCount(); arrowIndex++) {
                // setup projectile
                PersistentProjectileEntity arrow = null;
                ProjectileEntity projectile;
                float speed;
                if (ammo.isOf(Items.FIREWORK_ROCKET)) {
                    // TODO: don't hardcode fireworks, perhaps use a map or a JSON behavior list
                    projectile = new FireworkRocketEntity(level, ammo, player, player.getX(), player.getEyeY() - 0.15f, player.getZ(), true);
                    speed = 1.5f;
                    damage += 3;
                } else {
                    ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem a ? a : (ArrowItem) Items.ARROW;
                    arrow = arrowItem.createArrow(level, ammo, player);
                    projectile = arrow;
                    arrow.setCritical(true);
                    arrow.setSound(SoundEvents.ITEM_CROSSBOW_HIT);
                    arrow.setShotFromCrossbow(true);
                    speed = 3f;
                    damage += 1;

                    // vanilla arrows have a base damage of 2, cancel that out then add in our base damage to account for custom arrows with higher base damage
                    float baseArrowDamage = (float) (arrow.getDamage() - 2 + tool.getStats().get(ToolStats.PROJECTILE_DAMAGE));
                    arrow.setDamage(ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.PROJECTILE_DAMAGE, baseArrowDamage));

                    // fortunately, don't need to deal with vanilla infinity here, our infinity was dealt with during loading
                    if (creative) {
                        arrow.pickupType = PickupPermission.CREATIVE_ONLY;
                    }
                }

                // TODO: can we get piglins/illagers to use our crossbow?

                // setup projectile
                Vector3f targetVector = player.getRotationVec(1.0f).toVector3f();
                float angle = startAngle + (10 * arrowIndex);
                targetVector.rotate(new Quaterniond(new Vector3f(player.getOppositeRotationVector(1.0f)), angle, true));
                projectile.setVelocity(targetVector.x, targetVector.y, targetVector.z, velocity * speed, inaccuracy);

                // add modifiers to the projectile, will let us use them on impact
                ModifierNBT modifiers = tool.getModifiers();
                projectile.getCapability(EntityModifierCapability.CAPABILITY).ifPresent(cap -> cap.setModifiers(modifiers));

                // fetch the persistent data for the arrow as modifiers may want to store data
                NamespacedNBT projectileData = PersistentDataCapability.getOrWarn(projectile);

                // let modifiers set properties
                for (ModifierEntry entry : modifiers.getModifiers()) {
                    entry.getHook(ModifierHooks.PROJECTILE_LAUNCH).onProjectileLaunch(tool, entry, player, projectile, arrow, projectileData, arrowIndex == primaryIndex);
                }

                // finally, fire the projectile
                level.spawnEntity(projectile);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0F, getRandomShotPitch(angle, player.getRandom()));
            }

            // clear the ammo, damage the bow
            tool.getPersistentData().remove(KEY_CROSSBOW_AMMO);
            ToolDamageUtil.damageAnimated(tool, damage, player, hand);

            // stats
            if (player instanceof ServerPlayerEntity serverPlayer) {
                Criteria.SHOT_CROSSBOW.trigger(serverPlayer, player.getStackInHand(hand));
                serverPlayer.incrementStat(Stats.USED.getOrCreateStat(tool.getItem()));
            }
        }
    }

    @Override
    public void onStoppedUsing(ItemStack bow, World level, LivingEntity living, int chargeRemaining) {
        // clear zoom regardless, does not matter if the tool broke, we should not be zooming
        ScopeModifier.stopScoping(living);
        if (!(living instanceof PlayerEntity player)) {
            return;
        }
        ToolStack tool = ToolStack.from(bow);
        ModDataNBT persistentData = tool.getPersistentData();
        if (tool.isBroken() || persistentData.contains(KEY_CROSSBOW_AMMO, NbtElement.COMPOUND_TYPE)) {
            return;
        }

        // did we charge enough?
        int drawtime = persistentData.getInt(KEY_DRAWTIME);
        persistentData.remove(KEY_DRAWTIME);
        if ((getMaxUseTime(bow) - chargeRemaining) < drawtime) {
            return;
        }

        // find ammo and store it on the bow
        ItemStack ammo = BowAmmoModifierHook.findAmmo(tool, bow, player, getHeldProjectiles());
        if (!ammo.isEmpty()) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
            if (!level.isClient) {
                NbtCompound ammoNBT = ammo.writeNbt(new NbtCompound());
                persistentData.put(KEY_CROSSBOW_AMMO, ammoNBT);
                // if the crossbow broke during loading, fire immediately
                if (tool.isBroken()) {
                    fireCrossbow(tool, player, player.getActiveHand(), ammoNBT);
                }
            }
        }
    }

    @Override
    public List<Text> getStatInformation(IToolStackView tool, @Nullable PlayerEntity player, List<Text> tooltips, TooltipKey key, TooltipContext tooltipFlag) {
        tooltips = super.getStatInformation(tool, player, tooltips, key, tooltipFlag);

        // if we have ammo, render that in the tooltip
        NbtCompound heldAmmo = tool.getPersistentData().getCompound(KEY_CROSSBOW_AMMO);
        if (!heldAmmo.isEmpty()) {
            ItemStack heldStack = ItemStack.fromNbt(heldAmmo);
            if (!heldStack.isEmpty()) {
                // basic info: item and count
                MutableText component = Text.translatable(PROJECTILE_KEY);
                int count = heldStack.getCount();
                if (count > 1) {
                    component.append(" " + count + " ");
                } else {
                    component.append(" ");
                }
                tooltips.add(component.append(heldStack.toHoverableText()));

                // copy the stack's tooltip if advanced
                if (tooltipFlag.isAdvanced() && player != null) {
                    List<Text> nestedTooltip = new ArrayList<>();
                    heldStack.getItem().appendTooltip(heldStack, player.getWorld(), nestedTooltip, tooltipFlag);
                    for (Text nested : nestedTooltip) {
                        tooltips.add(Text.literal("  ").append(nested).formatted(Formatting.GRAY));
                    }
                }
            }
        }
        return tooltips;
    }
}
