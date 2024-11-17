package slimeknights.tconstruct.tools.network;

import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.network.PacketByteBuf;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

/**
 * Serializer for fluid stack data in entities
 */
public class FluidDataSerializer implements TrackedDataHandler<FluidStack> {
    @Override
    public void write(PacketByteBuf buffer, FluidStack stack) {
        buffer.writeFluidStack(stack);
    }

    @Override
    public FluidStack read(PacketByteBuf buffer) {
        return buffer.readFluidStack();
    }

    @Override
    public FluidStack copy(FluidStack stack) {
        return stack.copy();
    }
}
