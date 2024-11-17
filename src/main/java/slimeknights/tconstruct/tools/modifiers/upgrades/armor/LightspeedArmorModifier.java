package slimeknights.tconstruct.tools.modifiers.upgrades.armor;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ArmorWalkModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.UUID;

public class LightspeedArmorModifier extends Modifier implements ArmorWalkModifierHook, EquipmentChangeModifierHook, TooltipModifierHook {
    /**
     * UUID for speed boost
     */
    private static final UUID ATTRIBUTE_BONUS = UUID.fromString("8790747b-6654-4bd8-83c7-dbe9ae04c0ca");

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.BOOT_WALK, ModifierHooks.EQUIPMENT_CHANGE, ModifierHooks.TOOLTIP);
    }

    @Override
    public void onWalk(IToolStackView tool, ModifierEntry modifier, LivingEntity living, BlockPos prevPos, BlockPos newPos) {
        // no point trying if not on the ground
        if (tool.isBroken() || !living.isOnGround() || living.getWorld().isClient) {
            return;
        }
        // must have speed
        EntityAttributeInstance attribute = living.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        // start by removing the attribute, we are likely going to give it a new number
        if (attribute.getModifier(ATTRIBUTE_BONUS) != null) {
            attribute.removeModifier(ATTRIBUTE_BONUS);
        }

        // not above air
        Vec3d vecPos = living.getPos();
        BlockPos pos = new BlockPos(vecPos.x, vecPos.y + 0.5f, vecPos.z);
        int light = living.world.getBrightness(LightType.BLOCK, pos);
        if (light > 5) {
            int scaledLight = light - 5;
            attribute.addTemporaryModifier(new EntityAttributeModifier(ATTRIBUTE_BONUS, "tconstruct.modifier.lightspeed", scaledLight * 0.0015f * modifier.getEffectiveLevel(), Operation.ADDITION));

            // damage boots
            if (RANDOM.nextFloat() < (0.005f * scaledLight)) {
                ToolDamageUtil.damageAnimated(tool, 1, living, EquipmentSlot.FEET);
            }
        }
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        // remove boost when boots are removed
        LivingEntity livingEntity = context.getEntity();
        if (context.getChangedSlot() == EquipmentSlot.FEET) {
            IToolStackView newTool = context.getReplacementTool();
            // damaging the tool will trigger this hook, so ensure the new tool has the same level
            if (newTool == null || newTool.isBroken() || newTool.getModifier(this).getEffectiveLevel() != modifier.getEffectiveLevel()) {
                EntityAttributeInstance attribute = livingEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (attribute != null && attribute.getModifier(ATTRIBUTE_BONUS) != null) {
                    attribute.removeModifier(ATTRIBUTE_BONUS);
                }
            }
        }
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey key, TooltipContext tooltipFlag) {
        // multiplies boost by 10 and displays as a percent as the players base movement speed is 0.1 and is in unknown units
        // percentages make sense
        float boost;
        float level = modifier.getEffectiveLevel();
        if (player != null && key == TooltipKey.SHIFT) {
            int light = player.world.getBrightness(LightType.BLOCK, player.getBlockPos());
            boost = 0.015f * (light - 5) * level;
        } else {
            boost = 0.15f * level;
        }
        if (boost > 0) {
            TooltipModifierHook.addPercentBoost(this, this.getDisplayName(), boost, tooltip);
        }
    }
}
