package slimeknights.tconstruct.library.tools.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.utils.Util;

/**
 * Object for common context for weapon attack hooks
 */
@Getter
@RequiredArgsConstructor
public class ToolAttackContext {
    /**
     * Entity doing the attacking
     */
    @NotNull
    private final LivingEntity attacker;
    /**
     * Player doing the attacking, null if not a player
     */
    @Nullable
    private final PlayerEntity playerAttacker;
    /**
     * Hand containing the tool
     */
    @NotNull
    private final Hand hand;
    @NotNull
    private final EquipmentSlot slotType;
    /**
     * Originally targeted entity, may be different from {@link #getTarget()} for multipart entities
     */
    @NotNull
    private final Entity target;
    /**
     * Target entity
     */
    @Nullable
    private final LivingEntity livingTarget;
    /**
     * If true, attack is a critical hit
     */
    private final boolean isCritical;
    /**
     * Current attack cooldown
     */
    private final float cooldown;
    /**
     * If true, this is a secondary attack, such as for scythes
     */
    private final boolean isExtraAttack;

    public ToolAttackContext(LivingEntity attacker, @Nullable PlayerEntity playerAttacker, Hand hand, Entity target, @Nullable LivingEntity livingTarget, boolean isCritical, float cooldown, boolean isExtraAttack) {
        this(attacker, playerAttacker, hand, Util.getSlotType(hand), target, livingTarget, isCritical, cooldown, isExtraAttack);
    }

    /**
     * Returns true if this attack is fully charged
     */
    public boolean isFullyCharged() {
        return getCooldown() > 0.9f;
    }
}
