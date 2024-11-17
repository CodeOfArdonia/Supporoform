package slimeknights.mantle.fluid.tooltip;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataOutput.OutputType;
import net.minecraft.data.DataWriter;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.GenericDataProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Provider for fluid tooltip information
 */
@SuppressWarnings({"unused", "SameParameterValue"})  // API
public abstract class AbstractFluidTooltipProvider extends GenericDataProvider {
    private final Map<Identifier, Identifier> redirects = new HashMap<>();
    private final Map<Identifier, FluidUnitListBuilder> builders = new HashMap<>();
    private final String modId;

    public AbstractFluidTooltipProvider(DataOutput output, String modId) {
        super(output, OutputType.RESOURCE_PACK, FluidTooltipHandler.FOLDER, FluidTooltipHandler.GSON);
        this.modId = modId;
    }

    /**
     * Adds all relevant fluids to the maps
     */
    protected abstract void addFluids();

    @Override
    public final CompletableFuture<?> run(DataWriter cache) {
        this.addFluids();
        return this.allOf(Stream.concat(
                this.builders.entrySet().stream().map(entry -> this.saveJson(cache, entry.getKey(), entry.getValue().build())),
                this.redirects.entrySet().stream().map(entry -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("redirect", entry.getValue().toString());
                    return this.saveJson(cache, entry.getKey(), json);
                })));
    }


    /* Helpers */

    /**
     * Creates a ResourceLocation for the local mod
     */
    protected Identifier id(String name) {
        return new Identifier(this.modId, name);
    }

    /**
     * Adds a fluid to the builder
     */
    protected FluidUnitListBuilder add(Identifier id, @Nullable TagKey<Fluid> tag) {
        if (this.redirects.containsKey(id)) {
            throw new IllegalArgumentException(id + " is already registered as a redirect");
        }
        FluidUnitListBuilder newBuilder = new FluidUnitListBuilder(tag);
        FluidUnitListBuilder original = this.builders.put(id, newBuilder);
        if (original != null) {
            throw new IllegalArgumentException(id + " is already registered");
        }
        return newBuilder;
    }

    /**
     * Adds a fluid to the builder
     */
    protected FluidUnitListBuilder add(String id, TagKey<Fluid> tag) {
        return this.add(this.id(id), tag);
    }

    /**
     * Adds a fluid to the builder using the tag name as the ID
     */
    protected FluidUnitListBuilder add(TagKey<Fluid> tag) {
        return this.add(this.id(tag.id().getPath()), tag);
    }

    /**
     * Adds a fluid to the builder with no tag
     */
    protected FluidUnitListBuilder add(Identifier id) {
        return this.add(id, null);
    }

    /**
     * Adds a fluid to the builder with no tag
     */
    protected FluidUnitListBuilder add(String id) {
        return this.add(this.id(id), null);
    }

    /**
     * Adds a redirect from a named builder to a target
     */
    protected void addRedirect(Identifier id, Identifier target) {
        if (this.builders.containsKey(id)) {
            throw new IllegalArgumentException(id + " is already registered as a unit list");
        }
        Identifier original = this.redirects.put(id, target);
        if (original != null) {
            throw new IllegalArgumentException(id + " is already redirecting to " + original);
        }
    }

    /**
     * Builder for a unit list
     */
    @SuppressWarnings("unused")
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    protected class FluidUnitListBuilder {
        @Nullable
        private final TagKey<Fluid> tag;
        private final ImmutableList.Builder<FluidUnit> units = ImmutableList.builder();

        /**
         * Adds a unit with a full translation key
         */
        public FluidUnitListBuilder addUnitRaw(String key, int amount) {
            this.units.add(new FluidUnit(key, amount));
            return this;
        }

        /**
         * Adds a unit local to the current mod
         */
        public FluidUnitListBuilder addUnit(String key, int amount) {
            return this.addUnitRaw(Util.createTranslationKey("gui", AbstractFluidTooltipProvider.this.id("fluid." + key)), amount);
        }

        /**
         * Adds a unit local to the given mod
         */
        public FluidUnitListBuilder addUnit(String key, String domain, int amount) {
            return this.addUnitRaw(Util.createTranslationKey("gui", new Identifier(domain, "fluid." + key)), amount);
        }

        /**
         * Builds the final instance
         */
        private FluidUnitList build() {
            return new FluidUnitList(this.tag, this.units.build());
        }
    }
}
