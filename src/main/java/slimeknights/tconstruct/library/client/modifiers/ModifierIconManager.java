package slimeknights.tconstruct.library.client.modifiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import slimeknights.mantle.data.listener.IEarlySafeManagerReloadListener;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * Class handling the loading of modifier UI icons
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Log4j2
public class ModifierIconManager implements IEarlySafeManagerReloadListener {
    /**
     * Icon file to load, has merging behavior but forge prevents multiple mods from loading the same file
     */
    private static final String ICONS = "tinkering/modifier_icons.json";
    /**
     * First layer of the default icon, will be tinted
     */
    private static final Identifier DEFAULT_PAGES = TConstruct.getResource("gui/modifiers/default_pages");
    /**
     * Second layer of the default icon, will be tinted
     */
    private static final Identifier DEFAULT_COVER = TConstruct.getResource("gui/modifiers/default_cover");
    /**
     * Instance of this manager
     */
    public static final ModifierIconManager INSTANCE = new ModifierIconManager();

    /**
     * Map of icons for each modifier
     */
    private static Map<ModifierId, List<Identifier>> modifierIcons = Collections.emptyMap();

    /**
     * Initializes this manager, registering it relevant event busses
     */
    public static void init() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(ModifierIconManager::textureStitch);
        bus.addListener(ModifierIconManager::onResourceManagerRegister);
    }

    /**
     * Called on resource manager build to add the manager
     */
    private static void onResourceManagerRegister(RegisterClientReloadListenersEvent manager) {
        manager.registerReloadListener(INSTANCE);
    }

    /**
     * Called on texture stitch to add the new textures
     */
    private static void textureStitch(TextureStitchEvent.Pre event) {
        if (event.getAtlas().location().equals(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)) {
            // temporary workaround to the fact that texture stitching might run before the resource loader
            if (modifierIcons.isEmpty()) {
                INSTANCE.onReloadSafe(MinecraftClient.getInstance().getResourceManager());
            }
            Consumer<Identifier> spriteAdder = event::addSprite;
            modifierIcons.values().forEach(list -> list.forEach(spriteAdder));
            event.addSprite(DEFAULT_COVER);
            event.addSprite(DEFAULT_PAGES);
        }
    }

    @Override
    public void onReloadSafe(ResourceManager manager) {
        // start building the model map
        Map<ModifierId, List<Identifier>> icons = new HashMap<>();

        // get a list of files from all namespaces
        List<JsonObject> jsonFiles = JsonHelper.getFileInAllDomainsAndPacks(manager, ICONS, null);
        // first object is bottom most pack, so upper resource packs will replace it
        for (int i = jsonFiles.size() - 1; i >= 0; i--) {
            JsonObject json = jsonFiles.get(i);
            // right now just do simply key value pairs
            for (Entry<String, JsonElement> entry : json.entrySet()) {
                // get a valid name
                String key = entry.getKey();
                ModifierId name = ModifierId.tryParse(key);
                if (name == null) {
                    log.error("Skipping invalid modifier key " + key + " as it is not a valid resource location");
                    // ensure it's not already parsed
                } else if (!icons.containsKey(name)) {
                    // get a valid element, remove if null, error if not primitive
                    JsonElement element = entry.getValue();
                    if (element.isJsonNull()) {
                        icons.remove(name);
                    } else if (element.isJsonArray()) {
                        // list of paths, renders one after another
                        JsonArray array = element.getAsJsonArray();
                        try {
                            icons.put(name, JsonHelper.parseList(array, key, JsonHelper::convertToResourceLocation));
                        } catch (JsonSyntaxException e) {
                            log.error("Skipping invalid modifier " + key + " due to error parsing path list: ", e);
                        }
                    } else if (element.isJsonPrimitive()) {
                        // primitive means texture path
                        Identifier path = Identifier.tryParse(element.getAsString());
                        if (path != null) {
                            icons.put(name, Collections.singletonList(path));
                        } else {
                            log.error("Skipping invalid modifier " + key + " as the path is invalid");
                        }
                    } else {
                        log.error("Skipping key " + key + " as the value is not a valid path");
                    }
                }
            }
        }
        // replace the map
        modifierIcons = icons;
    }

    /**
     * Renders a modifier icon at the given location
     *
     * @param context  Matrix stack instance
     * @param modifier Modifier to draw
     * @param x        X offset
     * @param y        Y offset
     * @param z        Render depth offset, typically 100 is good
     * @param size     Size to render, 16 is default
     */
    public static void renderIcon(DrawContext context, Modifier modifier, int x, int y, int z, int size) {
        RenderUtils.setup(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        SpriteAtlasTexture atlas = MinecraftClient.getInstance().getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

        List<Identifier> icons = modifierIcons.getOrDefault(modifier.getId(), Collections.emptyList());
        if (!icons.isEmpty()) {
            for (Identifier icon : icons) {
                context.drawSprite(x, y, z, size, size, atlas.getSprite(icon));
            }
        } else {
            context.drawSprite(x, y, z, size, size, atlas.getSprite(DEFAULT_PAGES));
            RenderUtils.setColorRGBA(0xFF000000 | modifier.getColor());
            context.drawSprite(x, y, z, size, size, atlas.getSprite(DEFAULT_COVER));
            RenderUtils.setColorRGBA(-1);
        }
    }
}
