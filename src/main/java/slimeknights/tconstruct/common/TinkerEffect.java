package slimeknights.tconstruct.common;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;

/**
 * Effect extension with a few helpers
 */
public class TinkerEffect extends StatusEffect {
    /**
     * If true, effect is visible, false for hidden
     */
    private final boolean show;

    public TinkerEffect(StatusEffectCategory typeIn, boolean show) {
        this(typeIn, 0xffffff, show);
    }

    public TinkerEffect(StatusEffectCategory typeIn, int color, boolean show) {
        super(typeIn, color);
        this.show = show;
    }

    // override to change return type
    @Override
    public TinkerEffect addAttributeModifier(EntityAttribute pAttribute, String pUuid, double pAmount, Operation pOperation) {
        super.addAttributeModifier(pAttribute, pUuid, pAmount, pOperation);
        return this;
    }

    /* Visibility */

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(StatusEffectInstance effect) {
                return show;
            }

            @Override
            public boolean isVisibleInGui(StatusEffectInstance effect) {
                return show;
            }
        });
    }

    /* Helpers */

    /**
     * Applies this potion to an entity
     *
     * @param entity   Entity
     * @param duration Duration
     * @return Applied instance
     */
    public StatusEffectInstance apply(LivingEntity entity, int duration) {
        return this.apply(entity, duration, 0);
    }

    /**
     * Applies this potion to an entity
     *
     * @param entity   Entity
     * @param duration Duration
     * @param level    Effect level
     * @return Applied instance
     */
    public StatusEffectInstance apply(LivingEntity entity, int duration, int level) {
        return this.apply(entity, duration, level, false);
    }

    /**
     * Applies this potion to an entity
     *
     * @param entity   Entity
     * @param duration Duration
     * @param level    Effect level
     * @param showIcon If true, shows an icon in the HUD
     * @return Applied instance
     */
    public StatusEffectInstance apply(LivingEntity entity, int duration, int level, boolean showIcon) {
        StatusEffectInstance effect = new StatusEffectInstance(this, duration, level, false, false, showIcon);
        entity.addStatusEffect(effect);
        return effect;
    }

    /**
     * Gets the level of the effect on the entity, or -1 if not active
     *
     * @param entity Entity to check
     * @return Level, or -1 if inactive
     */
    public int getLevel(LivingEntity entity) {
        StatusEffectInstance effect = entity.getStatusEffect(this);
        if (effect != null) {
            return effect.getAmplifier();
        }
        return -1;
    }

}
