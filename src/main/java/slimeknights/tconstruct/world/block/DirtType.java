package slimeknights.tconstruct.world.block;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.world.level.material.MaterialColor;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.block.SlimeType;

import java.util.Locale;

/**
 * Variants of slimy dirt
 */
@Getter
public enum DirtType implements StringIdentifiable {
    EARTH(ToolMaterials.STONE, MaterialColor.GRASS),
    SKY(ToolMaterials.GOLD, MaterialColor.WARPED_STEM),
    ICHOR(ToolMaterials.IRON, MaterialColor.TERRACOTTA_LIGHT_BLUE),
    ENDER(ToolMaterials.DIAMOND, MaterialColor.TERRACOTTA_ORANGE),
    VANILLA(ToolMaterials.WOOD, MaterialColor.DIRT);

    /**
     * Dirt types added by the mod
     */
    public static final DirtType[] TINKER = {EARTH, SKY, ICHOR, ENDER};

    /**
     * Tier needed to harvest dirt blocks of this type
     */
    private final ToolMaterials harvestTier;
    /**
     * Color for this block on maps
     */
    private final MaterialColor mapColor;
    @Getter
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);

    /* Tags */
    /**
     * Tag for dirt blocks of this type, including blocks with grass on top
     */
    private final TagKey<Block> blockTag;

    DirtType(ToolMaterials harvestTier, MaterialColor mapColor) {
        this.harvestTier = harvestTier;
        this.mapColor = mapColor;
        this.blockTag = TinkerTags.Blocks.tag("slimy_soil/" + this.asString());
    }

    private SlimeType slimeType;

    /**
     * Gets the slime type for this dirt type
     */
    @Nullable
    public SlimeType asSlime() {
        if (this.slimeType == null && this != VANILLA) {
            this.slimeType = SlimeType.values()[this.ordinal()];
        }
        return this.slimeType;
    }
}
