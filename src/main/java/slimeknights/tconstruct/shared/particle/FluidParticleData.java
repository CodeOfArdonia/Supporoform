package slimeknights.tconstruct.shared.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.fluids.FluidType;

/**
 * Particle data for a fluid particle
 */
@RequiredArgsConstructor
public class FluidParticleData implements ParticleEffect {
    private static final DynamicCommandExceptionType UNKNOWN_FLUID = new DynamicCommandExceptionType(arg -> Text.translatable("command.tconstruct.fluid.not_found", arg));
    private static final ParticleEffect.Factory<FluidParticleData> DESERIALIZER = new ParticleEffect.Factory<>() {
        @Override
        public FluidParticleData read(ParticleType<FluidParticleData> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int i = reader.getCursor();
            Identifier id = Identifier.fromCommandInput(reader);
            Fluid fluid = Registries.FLUID.getOrEmpty(id).orElseThrow(() -> {
                reader.setCursor(i);
                return UNKNOWN_FLUID.createWithContext(reader, id.toString());
            });
            NbtCompound nbt = null;
            if (reader.canRead() && reader.peek() == '{') {
                nbt = new StringNbtReader(reader).parseCompound();
            }
            return new FluidParticleData(type, new FluidStack(fluid, FluidType.BUCKET_VOLUME, nbt));
        }

        @Override
        public FluidParticleData read(ParticleType<FluidParticleData> type, PacketByteBuf buffer) {
            return new FluidParticleData(type, FluidStack.readFromPacket(buffer));
        }
    };

    @Getter
    private final ParticleType<FluidParticleData> type;
    @Getter
    private final FluidStack fluid;

    @Override
    public void write(PacketByteBuf buffer) {
        fluid.writeToPacket(buffer);
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder();
        builder.append(Registries.PARTICLE_TYPE.getKey(getType()));
        builder.append(" ");
        builder.append(Registries.FLUID.getKey(this.fluid.getFluid()));
        NbtCompound nbt = this.fluid.getTag();
        if (nbt != null) {
            builder.append(nbt);
        }
        return builder.toString();
    }

    /**
     * Particle type for a fluid particle
     */
    public static class Type extends ParticleType<FluidParticleData> {
        public Type() {
            super(false, DESERIALIZER);
        }

        @Override
        public Codec<FluidParticleData> getCodec() {
            return FluidStack.CODEC.xmap(fluid -> new FluidParticleData(this, fluid), data -> data.fluid);
        }
    }
}
