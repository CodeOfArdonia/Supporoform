package slimeknights.tconstruct.library.materials.json;

import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * This json is mostly used for automatic consistency checks and for easier deserialization.
 * The actual stats deserialization is done in {@link slimeknights.tconstruct.library.materials.stats.MaterialStatsManager}
 */
@RequiredArgsConstructor
public class MaterialStatJson {
    @Nullable
    private final Map<Identifier, JsonElement> stats;

    public Map<Identifier, JsonElement> getStats() {
        if (this.stats == null) {
            return Collections.emptyMap();
        }
        return this.stats;
    }
}
