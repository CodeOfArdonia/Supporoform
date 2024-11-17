package slimeknights.tconstruct.library.json.math;

import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.floats.FloatStack;
import net.minecraft.network.PacketByteBuf;

/**
 * Stack operation which pushes a variable from the context
 */
record PushVariableOperation(int index) implements StackOperation {
    @Override
    public void perform(FloatStack stack, float[] variables) {
        // indices are validated during parsing, so this should never fail
        stack.push(variables[this.index]);
    }

    @Override
    public JsonPrimitive serialize(String[] variableNames) {
        return new JsonPrimitive('$' + variableNames[this.index]);
    }

    @Override
    public void toNetwork(PacketByteBuf buffer) {
        buffer.writeVarInt(PostFixOperator.VARIABLE_INDEX);
        buffer.writeVarInt(this.index);
    }
}
