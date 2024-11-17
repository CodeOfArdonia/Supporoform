package slimeknights.tconstruct.shared;

import net.minecraft.item.FoodComponent;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.block.FoliageType;

@SuppressWarnings("WeakerAccess")
public final class TinkerFood {
    private TinkerFood() {
    }

    /**
     * Bacon. What more is there to say?
     */
    public static final FoodComponent BACON = (new FoodComponent.Builder()).hunger(4).saturationModifier(0.6F).build();

    /**
     * Cheese is used for both the block and the ingot, eating the block returns 3 ingots
     */
    public static final FoodComponent CHEESE = (new FoodComponent.Builder()).hunger(3).saturationModifier(0.4F).build();

    /**
     * For the modifier
     */
    public static final FoodComponent JEWELED_APPLE = (new FoodComponent.Builder()).hunger(4).saturationModifier(1.2F).statusEffect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 0), 1.0F).effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2400, 0), 1.0F).alwaysEat().build();

    /* Cake block is set up to take food as a parameter */
    public static final FoodComponent EARTH_CAKE = new FoodComponent.Builder().hunger(1).saturationModifier(0.1f).alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.LUCK, 20 * 15, 0), 1.0f).build();
    public static final FoodComponent SKY_CAKE = new FoodComponent.Builder().hunger(1).saturationModifier(0.1f).alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.JUMP, 20 * 20, 1), 1.0f).build();
    public static final FoodComponent ICHOR_CAKE = new FoodComponent.Builder().hunger(3).saturationModifier(0.1f).alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 0), 1.0f).build();
    public static final FoodComponent BLOOD_CAKE = new FoodComponent.Builder().hunger(1).saturationModifier(0.2f).alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.REGENERATION, 50 * 3, 0), 1.0f).build();
    public static final FoodComponent MAGMA_CAKE = new FoodComponent.Builder().hunger(1).saturationModifier(0.2f).alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 30, 0), 1.0f).build();
    public static final FoodComponent ENDER_CAKE = new FoodComponent.Builder().hunger(2).saturationModifier(0.2f).alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.LEVITATION, 20 * 10, 0), 1.0f).build();

    public static final FoodComponent EARTH_BOTTLE = new FoodComponent.Builder().alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.LUCK, 1500), 1.0f).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900), 1.0f).build();
    public static final FoodComponent SKY_BOTTLE = new FoodComponent.Builder().alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.JUMP, 1800), 1.0f).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900), 1.0f).build();
    public static final FoodComponent ICHOR_BOTTLE = new FoodComponent.Builder().alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 500), 1.0f).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900), 1.0f).build();
    public static final FoodComponent ENDER_BOTTLE = new FoodComponent.Builder().alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.LEVITATION, 450), 1.0f).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 900), 1.0f).build();
    public static final FoodComponent MAGMA_BOTTLE = new FoodComponent.Builder().alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600), 1.0f).build();
    public static final FoodComponent VENOM_BOTTLE = new FoodComponent.Builder().alwaysEdible().statusEffect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800), 1.0f).effect(() -> new MobEffectInstance(MobEffects.POISON, 450), 1.0f).build();

    public static final FoodComponent MEAT_SOUP = new FoodComponent.Builder().hunger(8).saturationModifier(0.6f).build();

    /**
     * Gets the cake for the given slime type
     *
     * @param slime Slime type
     * @return Cake food
     */
    public static FoodComponent getCake(FoliageType slime) {
        return switch (slime) {
            case SKY -> SKY_CAKE;
            case ICHOR -> ICHOR_CAKE;
            case BLOOD -> BLOOD_CAKE;
            case ENDER -> ENDER_CAKE;
            default -> EARTH_CAKE;
        };
    }

    /**
     * Gets the cake for the given slime type
     *
     * @param slime Slime type
     * @return Cake food
     */
    public static FoodComponent getBottle(SlimeType slime) {
        return switch (slime) {
            case SKY -> SKY_BOTTLE;
            case ICHOR -> ICHOR_BOTTLE;
            case ENDER -> ENDER_BOTTLE;
            default -> EARTH_BOTTLE;
        };
    }
}