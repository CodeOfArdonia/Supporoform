package slimeknights.tconstruct.tools.modifiers.traits.general;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class SolarPoweredModifier extends NoLevelsModifier implements ToolDamageModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.TOOL_DAMAGE);
    }

    @Override
    public int getPriority() {
        return 185; // after tanned, before stoneshield
    }

    @Override
    public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder) {
        if (holder != null) {
            World world = holder.getEntityWorld();
            // note this may go negative, that is not a problem
            int skylight = world.getLightLevel(LightType.SKY, holder.getBlockPos()) - world.getAmbientDarkness();
            if (skylight > 0) {
                float chance = skylight * 0.05f; // up to a 75% chance at max sunlight
                int maxDamage = amount;
                // for each damage we will take, if the random number is below chance, reduce
                for (int i = 0; i < maxDamage; i++) {
                    if (RANDOM.nextFloat() < chance) {
                        amount--;
                    }
                }
            }
        }
        return amount;
    }
}
