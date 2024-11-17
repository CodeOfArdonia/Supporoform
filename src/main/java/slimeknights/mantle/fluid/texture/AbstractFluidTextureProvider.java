package slimeknights.mantle.fluid.texture;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataOutput.OutputType;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.Registry;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.JsonHelper;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Data provider for {@link FluidTexture}
 */
@SuppressWarnings("unused")
public abstract class AbstractFluidTextureProvider extends GenericDataProvider {
    private final Map<FluidType, FluidTexture.Builder> allTextures = new HashMap<>();
    private final Set<FluidType> ignore = new HashSet<>();
    @Nullable
    private final String modId;

    public AbstractFluidTextureProvider(DataGenerator generator, @Nullable String modId) {
        super(generator, OutputType.RESOURCE_PACK, FluidTextureManager.FOLDER, JsonHelper.DEFAULT_GSON);
        this.modId = modId;
    }

    @Override
    public final CompletableFuture<?> run(DataWriter cache) {
        this.addTextures();
        Registry<FluidType> fluidTypeRegistry = PortingLibFluids.FLUID_TYPES;

        // ensure we added textures for all our fluid types
        if (this.modId != null) {
            List<String> missing = fluidTypeRegistry.streamEntries().filter(entry -> entry.getKey().get().getValue().getNamespace().equals(this.modId) && !this.allTextures.containsKey(entry.value()) && !this.ignore.contains(entry.value())).map(e -> e.getKey().get().getValue().toString()).toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Missing fluid textures for: " + String.join(", ", missing));
            }
        }
        // save files
        return this.allOf(this.allTextures.entrySet().stream().map(entry -> this.saveJson(cache, Objects.requireNonNull(fluidTypeRegistry.getId(entry.getKey())), entry.getValue().build().serialize())));
    }

    /**
     * Override to add your textures at the proper time
     */
    public abstract void addTextures();

    /**
     * Create a new builder for the give fluid type
     */
    public FluidTexture.Builder texture(FluidType fluid) {
        return this.allTextures.computeIfAbsent(fluid, FluidTexture.Builder::new);
    }

    /**
     * Create a new builder for the give fluid type
     */
    public FluidTexture.Builder texture(FluidObject<?> fluid) {
        return this.texture(fluid.getType());
    }

    /**
     * Marks the given fluid type to be ignored by this texture provider
     */
    public void skip(FluidType fluid) {
        this.ignore.add(fluid);
    }

    /**
     * Marks the given fluid type to be ignored by this texture provider
     */
    public void skip(FluidObject<?> fluid) {
        this.skip(fluid.getType());
    }
}
