package slimeknights.tconstruct.library.json.variable.mining;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.math.ModifierFormula;
import slimeknights.tconstruct.library.json.variable.VariableFormula;
import slimeknights.tconstruct.library.json.variable.VariableFormulaLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.List;
import java.util.Map;

/**
 * Variable context for {@link slimeknights.tconstruct.library.modifiers.modules.mining.ConditionalMiningSpeedModule}
 */
public record MiningSpeedFormula(ModifierFormula formula, List<MiningSpeedVariable> variables, String[] variableNames,
                                 boolean percent) implements VariableFormula<MiningSpeedVariable> {
    /**
     * Variables for the modifier formula
     */
    public static final String[] VARIABLES = {"level", "speed", "multiplier", "original_speed"};
    /**
     * Loader instance
     */
    public static final RecordLoadable<MiningSpeedFormula> LOADER = new VariableFormulaLoadable<>(MiningSpeedVariable.LOADER, VARIABLES, (formula, variables, percent) -> new MiningSpeedFormula(formula, variables, EMPTY_STRINGS, percent));

    public MiningSpeedFormula(ModifierFormula formula, Map<String, MiningSpeedVariable> variables, boolean percent) {
        this(formula, List.copyOf(variables.values()), VariableFormula.getNames(variables), percent);
    }

    /**
     * Builds the arguments from the context
     */
    private float[] getArguments(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEvents.BreakSpeed event, @Nullable PlayerEntity player, @Nullable Direction sideHit, float baseSpeed, float newSpeed, float multiplier) {
        int size = variables.size();
        float[] arguments = VariableFormula.statModuleArguments(size, formula.processLevel(modifier), baseSpeed, newSpeed, multiplier * tool.getMultiplier(ToolStats.MINING_SPEED));
        for (int i = 0; i < size; i++) {
            arguments[4 + i] = variables.get(i).getValue(tool, event, player, sideHit);
        }
        return arguments;
    }

    /**
     * Runs this formula
     */
    public float apply(IToolStackView tool, ModifierEntry modifier, @Nullable PlayerEvents.BreakSpeed event, @Nullable PlayerEntity player, @Nullable Direction sideHit, float baseSpeed, float newSpeed, float multiplier) {
        return formula.apply(getArguments(tool, modifier, event, player, sideHit, baseSpeed, newSpeed, multiplier));
    }
}
