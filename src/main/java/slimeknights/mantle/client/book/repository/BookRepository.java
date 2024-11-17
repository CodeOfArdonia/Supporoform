package slimeknights.mantle.client.book.repository;

import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.book.data.SectionData;

import java.util.List;
import java.util.Optional;

public abstract class BookRepository {

    @SuppressWarnings("StaticInitializerReferencesSubClass") // will only occur in very specific threaded environment
    public static final BookRepository DUMMY = new DummyRepository();

    public abstract List<SectionData> getSections();

    @Nullable
    public Identifier getResourceLocation(@Nullable String path) {
        return this.getResourceLocation(path, false);
    }

    @Nullable
    public abstract Identifier getResourceLocation(@Nullable String path, boolean safe);

    /**
     * Gets a resource from the given location
     */
    public abstract Optional<Resource> getLocation(@Nullable Identifier loc);

    /**
     * Gets a resource from the given location, returning null if it does not exist
     */
    @Nullable
    public Resource getResource(@Nullable Identifier loc) {
        return this.getLocation(loc).orElse(null);
    }

    /**
     * Checks if the given resource exists
     */
    @SuppressWarnings("unused") // API
    public boolean resourceExists(@Nullable String location) {
        if (location == null) {
            return false;
        }

        return this.resourceExists(new Identifier(location));
    }

    /**
     * Checks if the given resource exists
     */
    public boolean resourceExists(@Nullable Identifier location) {
        return this.getLocation(location).isPresent();
    }

    public String resourceToString(@Nullable Resource resource) {
        return this.resourceToString(resource, true);
    }

    public abstract String resourceToString(@Nullable Resource resource, boolean skipComments);
}
