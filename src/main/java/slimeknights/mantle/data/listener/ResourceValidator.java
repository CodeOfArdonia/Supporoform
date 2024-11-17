package slimeknights.mantle.data.listener;

import com.google.common.collect.ImmutableSet;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility that handles checking if a resource exists in any resource pack.
 */
@SuppressWarnings("unused")  // API
public class ResourceValidator implements IEarlySafeManagerReloadListener, Predicate<Identifier> {
    private final String folder;
    private final int trim;
    private final String extension;
    protected Set<Identifier> resources;

    /**
     * Gets a resource validator instance
     *
     * @param folder    Folder to search
     * @param trim      Text to trim off resource locations
     * @param extension File extension
     */
    public ResourceValidator(String folder, String trim, String extension) {
        this.folder = folder;
        this.trim = trim.length() + 1;
        this.extension = extension;
        this.resources = ImmutableSet.of();
    }

    @Override
    public void onReloadSafe(ResourceManager manager) {
        int extensionLength = this.extension.length();
        this.resources = manager.findResources(this.folder, (loc) -> {
            // must have proper extension and contain valid characters
            return loc.getPath().endsWith(this.extension);
        }).keySet().stream().map((location) -> {
            String path = location.getPath();
            return new Identifier(location.getNamespace(), path.substring(this.trim, path.length() - extensionLength));
        }).collect(Collectors.toSet());
    }

    @Override
    public boolean test(Identifier location) {
        return this.resources.contains(location);
    }

    /**
     * Clears the resource cache, saves RAM as there could be a lot of locations
     */
    public void clear() {
        this.resources = ImmutableSet.of();
    }
}
