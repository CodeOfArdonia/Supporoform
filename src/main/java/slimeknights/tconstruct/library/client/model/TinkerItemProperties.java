package slimeknights.tconstruct.library.client.model;

import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * Properties for tinker tools
 */
public class TinkerItemProperties {
    /**
     * ID for broken property
     */
    private static final Identifier BROKEN_ID = TConstruct.getResource("broken");
    /**
     * Property declaring broken
     */
    private static final ClampedModelPredicateProvider BROKEN = (stack, level, entity, seed) -> ToolDamageUtil.isBroken(stack) ? 1 : 0;

    /**
     * ID for ammo property
     */
    private static final Identifier AMMO_ID = TConstruct.getResource("ammo");
    /**
     * Int declaring ammo type
     */
    private static final ClampedModelPredicateProvider AMMO = (stack, level, entity, seed) -> {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null) {
            NbtCompound persistentData = nbt.getCompound(ToolStack.TAG_PERSISTENT_MOD_DATA);
            if (!persistentData.isEmpty()) {
                NbtCompound ammo = persistentData.getCompound(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO.toString());
                if (!ammo.isEmpty()) {
                    // no sense having two keys for ammo, just set 1 for arrow, 2 for fireworks
                    return ammo.getString("id").equals(Registries.ITEM.getKey(Items.FIREWORK_ROCKET).toString()) ? 2 : 1;
                }
            }
        }
        return 0;
    };

    /**
     * ID for the pulling property
     */
    private static final Identifier CHARGING_ID = TConstruct.getResource("charging");
    /**
     * Boolean indicating the bow is pulling
     */
    private static final ClampedModelPredicateProvider CHARGING = (stack, level, holder, seed) -> {
        if (holder != null && holder.isUsingItem() && holder.getActiveItem() == stack) {
            UseAction anim = stack.getUseAction();
            if (anim == UseAction.BLOCK) {
                return 2;
            } else if (anim != UseAction.EAT && anim != UseAction.DRINK) {
                return 1;
            }
        }
        return 0;
    };
    /**
     * ID for the pull property
     */
    private static final Identifier CHARGE_ID = TConstruct.getResource("charge");
    /**
     * Property for bow pull amount
     */
    private static final ClampedModelPredicateProvider CHARGE = (stack, level, holder, seed) -> {
        if (holder == null || holder.getActiveItem() != stack) {
            return 0.0F;
        }
        int drawtime = ModifierUtil.getPersistentInt(stack, GeneralInteractionModifierHook.KEY_DRAWTIME, -1);
        return drawtime == -1 ? 0 : (float) (stack.getMaxUseTime() - holder.getItemUseTimeLeft()) / drawtime;
    };

    /**
     * Registers properties for a tool, including the option to have charge/block animations
     */
    public static void registerBrokenProperty(Item item) {
        ModelPredicateProviderRegistry.register(item, BROKEN_ID, BROKEN);
    }

    /**
     * Registers properties for a tool, including the option to have charge/block animations
     */
    public static void registerToolProperties(Item item) {
        registerBrokenProperty(item);
        ModelPredicateProviderRegistry.register(item, CHARGING_ID, CHARGING);
        ModelPredicateProviderRegistry.register(item, CHARGE_ID, CHARGE);
    }

    /**
     * Registers properties for a bow
     */
    public static void registerCrossbowProperties(Item item) {
        registerToolProperties(item);
        ModelPredicateProviderRegistry.register(item, AMMO_ID, AMMO);
    }
}
