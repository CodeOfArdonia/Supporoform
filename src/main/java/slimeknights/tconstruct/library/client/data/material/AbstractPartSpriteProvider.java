package slimeknights.tconstruct.library.client.data.material;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.client.data.util.AbstractSpriteReader;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.tools.stats.GripMaterialStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;
import slimeknights.tconstruct.tools.stats.LimbMaterialStats;
import slimeknights.tconstruct.tools.stats.StatlessMaterialStats;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Base class for listing sprites to generate
 */
@SuppressWarnings("SameParameterValue")
@RequiredArgsConstructor
public abstract class AbstractPartSpriteProvider {

    /**
     * List of created sprites
     */
    private final List<PartSpriteInfo> sprites = new ArrayList<>();
    private final List<ToolSpriteBuilder> toolSprites = new ArrayList<>();
    /**
     * If true, sprites have been added
     */
    private boolean added = false;
    /**
     * Default mod ID for helper functions
     */
    private final String modID;

    /**
     * Gets the name of these part sprites
     */
    public abstract String getName();

    /**
     * Function to add both sprites and materials
     */
    protected abstract void addAllSpites();


    /* Builder functions */

    /**
     * Adds a given texture to the list to generate
     *
     * @param sprite        Sprite name
     * @param requiredStats At least one of these stat types must be present for this sprite to be generated
     */
    protected void addTexture(Identifier sprite, MaterialStatsId requiredStats, boolean allowAnimated) {
        this.sprites.add(new PartSpriteInfo(sprite, requiredStats, allowAnimated));
    }

    /**
     * Adds a given texture to the list to generate
     *
     * @param sprite        Sprite name
     * @param requiredStats At least one of these stat types must be present for this sprite to be generated
     */
    protected void addTexture(Identifier sprite, MaterialStatsId requiredStats) {
        this.addTexture(sprite, requiredStats, true);
    }

    /**
     * Adds a given sprite to the list to generate, local to textures instead of tool
     *
     * @param name          Name relative to the mod
     * @param requiredStats At least one of these stat types must be present for this sprite to be generated
     */
    protected void addTexture(String name, MaterialStatsId requiredStats, boolean allowAnimated) {
        this.addTexture(new Identifier(this.modID, name), requiredStats, allowAnimated);
    }

    /**
     * Adds a given sprite to the list to generate, local to textures instead of tool
     *
     * @param name          Name relative to the mod
     * @param requiredStats At least one of these stat types must be present for this sprite to be generated
     */
    protected void addTexture(String name, MaterialStatsId requiredStats) {
        this.addTexture(new Identifier(this.modID, name), requiredStats);
    }

    /**
     * Adds a given sprite to the list to generated
     *
     * @param name          Name relative to the mod
     * @param requiredStats At least one of these stat types must be present for this sprite to be generated
     */
    protected void addSprite(String name, MaterialStatsId requiredStats) {
        this.addTexture(new Identifier(this.modID, "item/tool/" + name), requiredStats);
    }

    /**
     * Adds a sprite for a generic tool part from the parts folder
     *
     * @param name          Part name relative to item/tool/parts
     * @param requiredStats At least one of these stat types must be present for this part to be generated
     */
    protected void addPart(String name, MaterialStatsId requiredStats) {
        this.addSprite("parts/" + name, requiredStats);
    }

    /**
     * Adds a sprite requiring head stats
     */
    protected void addHead(String name) {
        this.addPart(name, HeadMaterialStats.ID);
    }

    /**
     * Adds a sprite requiring handle stats
     */
    protected void addHandle(String name) {
        this.addPart(name, HandleMaterialStats.ID);
    }

    /**
     * Adds a sprite requiring extra stats
     */
    protected void addBinding(String name) {
        this.addPart(name, StatlessMaterialStats.BINDING.getIdentifier());
    }

    /**
     * Adds a sprite requiring head stats
     */
    protected void addLimb(String name) {
        this.addPart(name, LimbMaterialStats.ID);
    }

    /**
     * Adds a sprite requiring head stats
     */
    protected void addBowstring(String name) {
        this.addPart(name, StatlessMaterialStats.BOWSTRING.getIdentifier());
    }

    /**
     * Create a builder for tool sprites
     */
    protected ToolSpriteBuilder buildTool(Identifier name) {
        ToolSpriteBuilder builder = new ToolSpriteBuilder(name);
        this.toolSprites.add(builder);
        return builder;
    }

    /**
     * Create a builder for tool sprites relative to the default mod ID
     */
    protected ToolSpriteBuilder buildTool(String name) {
        return this.buildTool(new Identifier(this.modID, name));
    }


    /* For use in the data generator */

    /**
     * Runs {@link #addAllSpites()} if not yet run
     */
    private void ensureSpritesAdded() {
        if (!this.added) {
            this.addAllSpites();
            this.toolSprites.forEach(ToolSpriteBuilder::build);
            this.toolSprites.clear();
            this.added = true;
        }
    }

    /**
     * Gets all sprites produced by this provider
     */
    public List<PartSpriteInfo> getSprites() {
        this.ensureSpritesAdded();
        return this.sprites;
    }

    /**
     * Closes all open images and resets all caches
     */
    public void cleanCache() {
        this.sprites.clear();
        this.added = false;
    }

    /**
     * Data class containing a sprite path, and different bases
     */
    @RequiredArgsConstructor
    public static class PartSpriteInfo {
        /**
         * Loadable instance
         */
        public static final RecordLoadable<PartSpriteInfo> LOADABLE = RecordLoadable.create(
                Loadables.RESOURCE_LOCATION.requiredField("path", i -> i.path),
                MaterialStatsId.PARSER.requiredField("stat_type", i -> i.statType),
                BooleanLoadable.INSTANCE.defaultField("allow_animated", true, false, i -> i.allowAnimated),
                PartSpriteInfo::new);
        /**
         * Loadable for a list, since its the main usage of this
         */
        public static final Loadable<List<PartSpriteInfo>> LIST_LOADABLE = LOADABLE.list(1);

        /**
         * Path to the base sprite
         */
        @Getter
        private final Identifier path;
        /**
         * Stat type of this part
         */
        @Getter
        private final MaterialStatsId statType;
        @Getter
        private final boolean allowAnimated;
        /**
         * Cache of fetched images for each sprite name
         */
        private transient final Map<String, NativeImage> sprites = new HashMap<>();

        /**
         * Gets the texture for the given fallback name, use empty string for the default
         */
        @Nullable
        public NativeImage getTexture(AbstractSpriteReader spriteReader, String name) {
            if (this.sprites.containsKey(name)) {
                return this.sprites.get(name);
            }
            // determine the path to try for the sprite
            Identifier fallbackPath = this.path;
            if (!name.isEmpty()) {
                fallbackPath = new Identifier(this.path.getNamespace(), this.path.getPath() + "_" + name);
            }
            // if the image exists, fetch it and return it
            NativeImage image = spriteReader.readIfExists(fallbackPath);
            this.sprites.put(name, image);
            return image;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    protected class ToolSpriteBuilder {
        private final Identifier name;
        private final Map<String, MaterialStatsId> parts = new LinkedHashMap<>();
        private boolean hasLarge = false;

        /**
         * Adds a part to the tool
         */
        public ToolSpriteBuilder addPart(String name, MaterialStatsId statTypes) {
            this.parts.put(name, statTypes);
            return this;
        }

        /**
         * Adds a part to the tool with a broken texture
         * 1.19 note: to simplify model generators, we changed from a broken prefix to a broken suffix for the part.
         * If you are not generating models and prefer less effort, just override this method.
         */
        public ToolSpriteBuilder addBreakablePart(String name, MaterialStatsId statTypes) {
            this.addPart(name, statTypes);
            this.addPart(name + "_broken", statTypes);
            return this;
        }

        /**
         * Adds a sprite requiring head stats
         */
        public ToolSpriteBuilder addHead(String name) {
            return this.addPart(name, HeadMaterialStats.ID);
        }

        /**
         * Adds a breakable part requiring head stats
         */
        public ToolSpriteBuilder addBreakableHead(String name) {
            return this.addBreakablePart(name, HeadMaterialStats.ID);
        }

        /**
         * Adds a sprite requiring handle stats
         */
        public ToolSpriteBuilder addHandle(String name) {
            return this.addPart(name, HandleMaterialStats.ID);
        }

        /**
         * Adds a sprite requiring extra stats
         */
        public ToolSpriteBuilder addBinding(String name) {
            return this.addPart(name, StatlessMaterialStats.BINDING.getIdentifier());
        }

        /**
         * Adds a sprite requiring limb stats
         */
        public ToolSpriteBuilder addLimb(String name) {
            return this.addPart(name, LimbMaterialStats.ID);
        }

        /**
         * Adds a sprite requiring grip stats
         */
        public ToolSpriteBuilder addGrip(String name) {
            return this.addPart(name, GripMaterialStats.ID);
        }

        /**
         * Adds a sprite requiring bowstring stats
         */
        public ToolSpriteBuilder addBowstring(String name) {
            return this.addPart(name, StatlessMaterialStats.BOWSTRING.getIdentifier());
        }

        /**
         * Adds a breakable sprite requiring bowstring stats
         */
        public ToolSpriteBuilder addBreakableBowstring(String name) {
            return this.addBreakablePart(name, StatlessMaterialStats.BOWSTRING.getIdentifier());
        }

        /**
         * Adds sprites for large parts as well
         */
        public ToolSpriteBuilder withLarge() {
            this.hasLarge = true;
            return this;
        }

        /**
         * Helper to add all parts for a size
         */
        private void addParts(String path) {
            for (Entry<String, MaterialStatsId> entry : this.parts.entrySet()) {
                AbstractPartSpriteProvider.this.addTexture(new Identifier(this.name.getNamespace(), "item/tool/" + path + "/" + entry.getKey()), entry.getValue());
            }
        }

        /**
         * Adds all sprites into the list of sprites
         */
        private void build() {
            this.addParts(this.name.getPath());
            if (this.hasLarge) {
                this.addParts(this.name.getPath() + "/large");
            }
        }
    }
}
