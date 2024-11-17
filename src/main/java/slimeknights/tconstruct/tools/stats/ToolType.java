package slimeknights.tconstruct.tools.stats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.Locale;

/**
 * Helper for registering the different effects for modifiers that change behavior based on the tool type
 */
@RequiredArgsConstructor
@Getter
public enum ToolType implements StringIdentifiable {
    MELEE(TinkerTags.Items.MELEE_WEAPON),
    HARVEST(TinkerTags.Items.HARVEST),
    RANGED(TinkerTags.Items.RANGED),
    ARMOR(TinkerTags.Items.ARMOR);

    public static final ToolType[] NO_MELEE = {HARVEST, RANGED, ARMOR};

    private final TagKey<Item> tag;
    private final String serializedName = this.name().toLowerCase(Locale.ROOT);

    @Nullable
    public static ToolType from(Item item, ToolType... types) {
        for (ToolType type : types) {
            if (RegistryHelper.contains(type.tag, item)) {
                return type;
            }
        }
        return null;
    }
}
