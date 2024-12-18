package slimeknights.tconstruct.library.json.variable.melee;

import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.math.ModifierFormula;
import slimeknights.tconstruct.library.json.variable.VariableFormula;
import slimeknights.tconstruct.library.json.variable.VariableFormulaLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.combat.ConditionalMeleeDamageModule;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * Variable context for {@link ConditionalMeleeDamageModule}
 */
public record MeleeFormula(ModifierFormula formula, List<MeleeVariable> variables, String[] variableNames,
                           boolean percent) implements VariableFormula<MeleeVariable> {
    /**
     * Variables for the modifier formula
     */
    public static final String[] VARIABLES = {"level", "damage", "multiplier", "base_damage"};
    /**
     * Loader instance
     */
    public static final RecordLoadable<MeleeFormula> LOADER = new VariableFormulaLoadable<>(MeleeVariable.LOADER, VARIABLES, (formula, variables, percent) -> new MeleeFormula(formula, variables, EMPTY_STRINGS, percent));

    public MeleeFormula(ModifierFormula formula, Map<String, MeleeVariable> variables, boolean percent) {
        this(formula, List.copyOf(variables.values()), VariableFormula.getNames(variables), percent);
    }

    /**
     * Builds the arguments from the context
     */
    private float[] getArguments(IToolStackView tool, ModifierEntry modifier, @Nullable ToolAttackContext context, @Nullable LivingEntity attacker, float baseDamage, float damage) {
        int size = this.variables.size();
        float[] arguments = VariableFormula.statModuleArguments(size, this.formula.processLevel(modifier), baseDamage, damage, tool.getMultiplier(ToolStats.ATTACK_DAMAGE));
        for (int i = 0; i < size; i++) {
            arguments[4 + i] = this.variables.get(i).getValue(tool, context, attacker);
        }
        return arguments;
    }

    /**
     * Runs this formula
     */
    public float apply(IToolStackView tool, ModifierEntry modifier, @Nullable ToolAttackContext context, @Nullable LivingEntity attacker, float baseDamage, float damage) {
        return this.formula.apply(this.getArguments(tool, modifier, context, attacker, baseDamage, damage));
    }
}
