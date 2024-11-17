package slimeknights.tconstruct.tools.modifiers.upgrades.melee;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraftforge.common.Tags;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ProcessLootModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipeCache;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

public class SeveringModifier extends Modifier implements ProcessLootModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROCESS_LOOT);
    }

    @Override
    public void processLoot(IToolStackView tool, ModifierEntry modifier, List<ItemStack> generatedLoot, LootContext context) {
        // if no damage source, probably not a mob
        // otherwise blocks breaking (where THIS_ENTITY is the player) start dropping player heads
        if (!context.hasParameter(LootContextParameters.DAMAGE_SOURCE)) {
            return;
        }

        // must have an entity
        Entity entity = context.get(LootContextParameters.THIS_ENTITY);
        if (entity != null) {
            // ensure no head so far
            if (generatedLoot.stream().noneMatch(stack -> stack.isIn(Tags.Items.HEADS))) {
                // find proper recipe
                List<SeveringRecipe> recipes = SeveringRecipeCache.findRecipe(context.getWorld().getRecipeManager(), entity.getType());
                if (!recipes.isEmpty()) {
                    // 5% chance per level, each luck level adds an extra 1% per severing level
                    float chance = modifier.getEffectiveLevel() * (0.05f + 0.01f * context.getLootingModifier());
                    // double chance for mobs such as ender dragons and the wither
                    if (entity.getType().isIn(TinkerTags.EntityTypes.RARE_MOBS)) {
                        chance *= 2;
                    }
                    for (SeveringRecipe recipe : recipes) {
                        ItemStack result = recipe.getOutput(entity);
                        if (!result.isEmpty() && RANDOM.nextFloat() < chance) {
                            // if count is not 1, its a random range from 1 to count
                            if (result.getCount() > 1) {
                                result.setCount(RANDOM.nextInt(result.getCount()) + 1);
                            }
                            generatedLoot.add(result);
                        }
                    }
                }
            }
        }
    }
}
