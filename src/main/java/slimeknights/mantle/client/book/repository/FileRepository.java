package slimeknights.mantle.client.book.repository;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.SectionData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FileRepository extends BookRepository {

    public final Identifier location;

    public FileRepository(Identifier location) {
        this.location = location;
    }

    @Override
    public List<SectionData> getSections() {
        return new ArrayList<>(Arrays.asList(BookLoader.getGson().fromJson(this.resourceToString(this.getResource(this.getResourceLocation("index.json"))), SectionData[].class)));
    }

    @Override
    public Identifier getResourceLocation(@Nullable String path, boolean safe) {
        if (path == null) {
            return safe ? new Identifier("") : null;
        }

        if (!path.contains(":")) {
            String langPath = null;

            //noinspection ConstantConditions - this was proven to be null once
            if (MinecraftClient.getInstance().getLanguageManager() != null && MinecraftClient.getInstance().getLanguageManager().getLanguage() != null) {
                langPath = MinecraftClient.getInstance().getLanguageManager().getLanguage();
            }

            String defaultLangPath = "en_us";

            Identifier res;

            // TODO: this can be optimized if we return the resource instead of the location, how feasible is that in practice?
            //noinspection ConstantConditions - see above
            if (langPath != null) {
                res = new Identifier(this.location + "/" + langPath + "/" + path);
                if (this.resourceExists(res)) {
                    return res;
                }
            }
            res = new Identifier(this.location + "/" + defaultLangPath + "/" + path);
            if (this.resourceExists(res)) {
                return res;
            }
            res = new Identifier(this.location + "/" + path);
            if (this.resourceExists(res)) {
                return res;
            }
        } else {
            Identifier res = new Identifier(path);
            if (this.resourceExists(res)) {
                return res;
            }
        }

        return safe ? new Identifier("") : null;
    }

    @Override
    public Optional<Resource> getLocation(@Nullable Identifier loc) {
        if (loc == null) {
            return Optional.empty();
        }
        return MinecraftClient.getInstance().getResourceManager().getResource(loc);
    }

    @Override
    public String resourceToString(@Nullable Resource resource, boolean skipComments) {
        if (resource == null) {
            return "";
        }

        try {
            Iterator<String> iterator = IOUtils.readLines(resource.getInputStream(), StandardCharsets.UTF_8).iterator();
            StringBuilder builder = new StringBuilder();

            boolean isLongComment = false;

            while (iterator.hasNext()) {
                String s = iterator.next().trim() + "\n";

                // Comment skipper
                if (skipComments) {
                    if (isLongComment) {
                        if (s.endsWith("*/")) {
                            isLongComment = false;
                        }
                        continue;
                    } else {
                        if (s.startsWith("/*")) {
                            isLongComment = true;
                            continue;
                        }
                    }
                    if (s.startsWith("//")) {
                        continue;
                    }
                }

                builder.append(s);
            }

            return builder.toString().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}
