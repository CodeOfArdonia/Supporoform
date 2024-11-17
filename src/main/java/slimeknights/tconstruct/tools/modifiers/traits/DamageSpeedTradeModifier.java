package slimeknights.tconstruct.tools.modifiers.traits;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Shared logic for jagged and stonebound. Trait boosts attack damage as it lowers mining speed.
 */
public class DamageSpeedTradeModifier extends Modifier implements AttributesModifierHook, TooltipModifierHook, BreakSpeedModifierHook {
    private static final Text MINING_SPEED = TConstruct.makeTranslation("armor_stat", "mining_speed");
    private final float multiplier;
    private final Lazy<UUID> uuid = Lazy.of(() -> UUID.nameUUIDFromBytes(this.getId().toString().getBytes()));
    private final Lazy<String> attributeName = Lazy.of(() -> {
        ResourceLocation id = this.getId();
        return id.getPath() + "." + id.getNamespace() + ".attack_damage";
    });

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOLTIP, ModifierHooks.ATTRIBUTES, ModifierHooks.BREAK_SPEED);
    }

    /**
     * Creates a new instance of
     *
     * @param multiplier Multiplier. Positive boosts damage, negative boosts mining speed
     */
    public DamageSpeedTradeModifier(float multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * Gets the multiplier for this modifier at the current durability and level
     */
    private double getMultiplier(IToolStackView tool, int level) {
        return Math.sqrt(tool.getDamage() * level / tool.getMultiplier(ToolStats.DURABILITY)) * this.multiplier;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEntity player, List<Text> tooltip, TooltipKey tooltipKey, TooltipContext tooltipFlag) {
        double boost = this.getMultiplier(tool, modifier.getLevel());
        if (boost != 0 && tool.hasTag(TinkerTags.Items.HARVEST)) {
            tooltip.add(this.applyStyle(Text.literal(Util.PERCENT_BOOST_FORMAT.format(-boost)).append(" ").append(MINING_SPEED)));
        }
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<EntityAttribute, EntityAttributeModifier> consumer) {
        if (slot == EquipmentSlot.MAINHAND) {
            double boost = this.getMultiplier(tool, modifier.getLevel());
            if (boost != 0) {
                // half boost for attack speed, its
                consumer.accept(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(this.uuid.get(), this.attributeName.get(), boost / 2, Operation.MULTIPLY_TOTAL));
            }
        }
    }

    @Override
    public void onBreakSpeed(IToolStackView tool, ModifierEntry modifier, BreakSpeed event, Direction sideHit, boolean isEffective, float miningSpeedModifier) {
        if (isEffective) {
            event.setNewSpeed((float) (event.getNewSpeed() * (1 - this.getMultiplier(tool, modifier.getLevel()))));
        }
    }
}
