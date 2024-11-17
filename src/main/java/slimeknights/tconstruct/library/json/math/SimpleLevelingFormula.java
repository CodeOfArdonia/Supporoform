package slimeknights.tconstruct.library.json.math;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketByteBuf;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

/**
 * Implementation of {@link ModifierFormula} that uses a simpler formula for more efficiency, since not everything needs the flexibility of post fix
 */
record SimpleLevelingFormula(LevelingValue leveling, FallbackFormula formula) implements ModifierFormula {
    @Override
    public float apply(float... arguments) {
        return this.formula.apply(arguments);
    }

    @Override
    public float processLevel(ModifierEntry modifier) {
        return this.leveling.compute(modifier.getEffectiveLevel());
    }

    @Override
    public JsonObject serialize(JsonObject json, String[] variableNames) {
        return this.leveling.serialize(json);
    }

    @Override
    public void toNetwork(PacketByteBuf buffer) {
        // -1 means simple formula, 0+ means post fix formula
        buffer.writeShort(-1);
        this.leveling.toNetwork(buffer);
    }
}
