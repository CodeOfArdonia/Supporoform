package slimeknights.tconstruct.library.client.model.tools;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.mapping.CompactLoadable;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.ReversedListBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.BakedUniqueGuiModel;
import slimeknights.tconstruct.library.client.modifiers.IBakedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelManager;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.worktable.ModifierSetWorktableRecipe;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Model handling all tools, both multipart and non.
 */
public class ToolModel implements IUnbakedGeometry<ToolModel> {
    /**
     * Shared loader instance
     */
    public static final IGeometryLoader<ToolModel> LOADER = ToolModel::deserialize;
    /**
     * Set of transform types that make tools render small
     */
    private static final BitSet SMALL_TOOL_TYPES = new BitSet();

    /**
     * Registers a new small tool transform type
     */
    public static synchronized TransformType registerSmallTool(TransformType type) {
        SMALL_TOOL_TYPES.set(type.ordinal());
        return type;
    }


    /**
     * Color handler instance for all tools, handles both material and modifier colors
     */
    public static final ItemColorProvider COLOR_HANDLER = (stack, index) -> {
        // TODO: reconsider material item colors, is there a usecase for dynamic colors as opposed to just an animated texture?
        if (index >= 0) {
            // for modifiers, we need the overrides instance to properly process
            BakedModel itemModel = MinecraftClient.getInstance().getItemRenderer().getModels().getModel(stack.getItem());
            if (itemModel != null && itemModel.getOverrides() instanceof MaterialOverrideHandler overrides) {
                ToolStack tool = ToolStack.from(stack);
                // modifier model indexes start at the last part
                int localIndex = 0;
                List<ModifierEntry> modifiers = tool.getUpgrades().getModifiers();
                ModifierEntry[] firsts = new ModifierEntry[overrides.firstModifiers.size()];
                for (int i = modifiers.size() - 1; i >= 0; i--) {
                    ModifierEntry entry = modifiers.get(i);
                    ModifierId id = entry.getId();
                    int firstIndex = FirstModifier.indexOf(overrides.firstModifiers, id);
                    if (firstIndex != -1) {
                        firsts[firstIndex] = entry;
                    } else {
                        // colors are assumed to not be sensitive to the model's large status
                        IBakedModifierModel modifierModel = overrides.modifierModels.get(entry.getId());
                        if (modifierModel != null) {
                            // indexes from [0,modelIndexes) are passed to this model
                            // if below the range, make the index model relative
                            // if above the range, add the count and let the next model handle it
                            int modelIndexes = modifierModel.getTintIndexes();
                            if (localIndex + modelIndexes > index) {
                                return modifierModel.getTint(tool, entry, index - localIndex);
                            }
                            localIndex += modelIndexes;
                        }
                    }
                }
                // first, add the first modifier tints
                for (int i = firsts.length - 1; i >= 0; i--) {
                    ModifierEntry entry = firsts[i];
                    FirstModifier first = overrides.firstModifiers.get(i);
                    if (entry != null || first.forced) {
                        IBakedModifierModel model = overrides.modifierModels.get(first.id);
                        if (model != null) {
                            int modelIndexes = model.getTintIndexes();
                            if (localIndex + modelIndexes > index) {
                                return model.getTint(tool, Objects.requireNonNullElse(entry, ModifierEntry.EMPTY), index - localIndex);
                            }
                            localIndex += modelIndexes;
                        }
                    }
                }
            }
        }
        return -1;
    };

    /**
     * Registers an item color handler for a part item
     *
     * @param colors Item colors instance
     * @param item   Material item
     */
    @SuppressWarnings("deprecation")  // yeah forge, you have nice event, this is happening during the event so its fine
    public static void registerItemColors(ItemColors colors, Supplier<? extends IModifiable> item) {
        colors.register(ToolModel.COLOR_HANDLER, item.get());
    }

    /**
     * Deserializes the model from JSON
     */
    public static ToolModel deserialize(JsonObject json, JsonDeserializationContext context) {
        List<ToolPart> parts = Collections.emptyList();
        if (json.has("parts")) {
            parts = JsonHelper.parseList(json, "parts", ToolPart::read);
        }
        boolean isLarge = net.minecraft.util.JsonHelper.getBoolean(json, "large", false);
        Vec2f offset = Vec2f.ZERO;
        if (json.has("large_offset")) {
            offset = MaterialModel.getVec2(json, "large_offset");
        }
        // modifier root fetching
        List<Identifier> smallModifierRoots = Collections.emptyList();
        List<Identifier> largeModifierRoots = Collections.emptyList();
        if (json.has("modifier_roots")) {
            // large model requires an object
            if (isLarge) {
                JsonObject modifierRoots = net.minecraft.util.JsonHelper.getObject(json, "modifier_roots");
                BiFunction<JsonElement, String, Identifier> parser = (element, string) -> new Identifier(net.minecraft.util.JsonHelper.asString(element, string));
                smallModifierRoots = JsonHelper.parseList(modifierRoots, "small", parser);
                largeModifierRoots = JsonHelper.parseList(modifierRoots, "large", parser);
            } else {
                // small requires an array
                smallModifierRoots = JsonHelper.parseList(json, "modifier_roots", (element, string) -> new Identifier(net.minecraft.util.JsonHelper.asString(element, string)));
            }
        }
        // modifiers first
        List<FirstModifier> firstModifiers = FirstModifier.LOADABLE.getOrDefault(json, "first_modifiers", List.of());
        return new ToolModel(parts, isLarge, offset, smallModifierRoots, largeModifierRoots, firstModifiers);
    }

    /**
     * List of tool parts in this model
     */
    private List<ToolPart> toolParts;
    /**
     * If true, this is a large tool and uses double resolution textures in hand
     */
    private final boolean isLarge;
    /**
     * Transform matrix to apply to child parts
     */
    private final Vec2f offset;
    /**
     * Location to fetch modifier textures for small variant
     */
    private final List<Identifier> smallModifierRoots;
    /**
     * Location to fetch modifier textures for large variant
     */
    private final List<Identifier> largeModifierRoots;
    /**
     * Modifiers that show first on tools, bypassing normal sort order
     */
    private final List<FirstModifier> firstModifiers;
    /**
     * Models for the relevant modifiers
     */
    private Map<ModifierId, IBakedModifierModel> modifierModels = Collections.emptyMap();

    public ToolModel(List<ToolPart> parts, boolean isLarge, Vec2f offset, List<Identifier> smallModifierRoots, List<Identifier> largeModifierRoots, List<FirstModifier> firstModifiers) {
        this.toolParts = parts;
        this.isLarge = isLarge;
        this.offset = offset;
        this.smallModifierRoots = smallModifierRoots;
        this.largeModifierRoots = largeModifierRoots;
        this.firstModifiers = firstModifiers;
    }

    @Override
    public Collection<SpriteIdentifier> getMaterials(JsonUnbakedModel owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        Set<SpriteIdentifier> allTextures = Sets.newHashSet();
        // default is just a single part named tool, no material
        if (toolParts.isEmpty()) {
            toolParts = ToolPart.DEFAULT_PARTS;
        }

        // after the above condition, we always have parts, so just iterate them
        for (ToolPart part : toolParts) {
            // if material variants, fetch textures from the material model
            if (part.hasMaterials()) {
                MaterialModel.getMaterialTextures(allTextures, owner, part.getName(false), null);
                if (isLarge) {
                    MaterialModel.getMaterialTextures(allTextures, owner, part.getName(true), null);
                }
            } else {
                // static texture
                allTextures.add(owner.resolveSprite(part.getName(false)));
                if (isLarge) {
                    allTextures.add(owner.resolveSprite(part.getName(true)));
                }
            }
        }
        // load modifier models
        modifierModels = ModifierModelManager.getModelsForTool(smallModifierRoots, isLarge ? largeModifierRoots : Collections.emptyList(), allTextures);

        return allTextures;
    }

    /**
     * adds quads for relevant modifiers
     *
     * @param spriteGetter   Sprite getter instance
     * @param modifierModels Map of modifier models
     * @param tool           Tool instance
     * @param quadConsumer   Consumer for finished quads
     * @param transforms     Transforms to apply
     * @param isLarge        If true, the quads are for a large tool
     */
    private static void addModifierQuads(Function<SpriteIdentifier, Sprite> spriteGetter, Map<ModifierId, IBakedModifierModel> modifierModels, List<FirstModifier> firstModifiers, IToolStackView tool, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels, AffineTransformation transforms, boolean isLarge) {
        if (!modifierModels.isEmpty()) {
            // keep a running tint index so models know where they should start, currently starts at 0 as the main model does not use tint indexes
            int modelIndex = 0;
            // reversed order to ensure the pixels is updated correctly
            List<ModifierEntry> modifiers = tool.getUpgrades().getModifiers();
            // keep track of the entry for each first modifier, as that may impact how it renders
            ModifierEntry[] firsts = new ModifierEntry[firstModifiers.size()];
            if (!modifiers.isEmpty()) {
                // last, add all regular modifiers
                Set<ModifierId> hidden = ModifierSetWorktableRecipe.getModifierSet(tool.getPersistentData(), TConstruct.getResource("invisible_modifiers"));
                for (int i = modifiers.size() - 1; i >= 0; i--) {
                    ModifierEntry entry = modifiers.get(i);
                    ModifierId modifier = entry.getModifier().getId();
                    int index = FirstModifier.indexOf(firstModifiers, modifier);
                    if (index != -1) {
                        // handle first modifiers later
                        firsts[index] = entry;
                    } else if (!hidden.contains(modifier)) {
                        IBakedModifierModel model = modifierModels.get(modifier);
                        if (model != null) {
                            // if the modifier is in the list, delay adding its quads, but keep the expected tint index
                            model.addQuads(tool, entry, spriteGetter, transforms, isLarge, modelIndex, quadConsumer, pixels);
                            modelIndex += model.getTintIndexes();
                        }
                    }
                }
            }
            // first, add the first modifiers
            for (int i = firsts.length - 1; i >= 0; i--) {
                ModifierEntry entry = firsts[i];
                FirstModifier first = firstModifiers.get(i);
                if (entry != null || first.forced) {
                    IBakedModifierModel model = modifierModels.get(first.id);
                    if (model != null) {
                        model.addQuads(tool, Objects.requireNonNullElse(entry, ModifierEntry.EMPTY), spriteGetter, transforms, isLarge, modelIndex, quadConsumer, pixels);
                        modelIndex += model.getTintIndexes();
                    }
                }
            }
        }
    }

    /**
     * Modifier that may be forced
     */
    private record FirstModifier(ModifierId id, boolean forced) {
        private static final Loadable<List<FirstModifier>> LOADABLE = CompactLoadable.of(
                RecordLoadable.create(ModifierId.PARSER.requiredField("name", FirstModifier::id), BooleanLoadable.INSTANCE.defaultField("forced", false, false, FirstModifier::forced), FirstModifier::new),
                ModifierId.PARSER.flatXmap(id -> new FirstModifier(id, false), FirstModifier::id),
                f -> !f.forced).list(0);

        /**
         * Gets the index of a modifier in the list
         */
        public static int indexOf(List<FirstModifier> list, ModifierId id) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).id.equals(id)) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Makes a model builder for the given context and overrides
     */
    private static IModelBuilder<?> makeModelBuilder(JsonUnbakedModel context, ModelOverrideList overrides, Sprite particle) {
        return IModelBuilder.of(context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(), context.getTransforms(), overrides, particle, MantleItemLayerModel.getDefaultRenderType(context));
    }

    /**
     * Same as {@link #bake(IGeometryBakingContext, ModelBakery, Function, ModelState, ItemOverrides, ResourceLocation)}, but uses fewer arguments and does not require an instance
     *
     * @param owner           Model configuration
     * @param spriteGetter    Sprite getter function
     * @param largeTransforms Transform to apply to the large parts. If null, only generates small parts
     * @param parts           List of tool parts in this tool
     * @param modifierModels  Map of modifier models for this tool
     * @param materials       Materials to use for the parts
     * @param tool            Tool instance for modifier parsing
     * @param overrides       Override instance to use, will either be empty or {@link MaterialOverrideHandler}
     * @return Baked model
     */
    private static BakedModel bakeInternal(JsonUnbakedModel owner, Function<SpriteIdentifier, Sprite> spriteGetter, @Nullable AffineTransformation largeTransforms,
                                           List<ToolPart> parts, Map<ModifierId, IBakedModifierModel> modifierModels, List<FirstModifier> firstModifiers,
                                           List<MaterialVariantId> materials, @Nullable IToolStackView tool, ModelOverrideList overrides) {
        AffineTransformation smallTransforms = AffineTransformation.identity();

        // TODO: would be nice to support render types per material/per modifier
        // small model is used in GUIs (though that will be filtered to just front faces) and in small tool contexts like casting blocks
        ReversedListBuilder<Collection<BakedQuad>> smallQuads = new ReversedListBuilder<>();
        ItemLayerPixels smallPixels = new ItemLayerPixels();
        // large model is used in all other places
        ReversedListBuilder<Collection<BakedQuad>> largeQuads = largeTransforms != null ? new ReversedListBuilder<>() : smallQuads;
        ItemLayerPixels largePixels = largeTransforms != null ? new ItemLayerPixels() : smallPixels;

        // add quads for all modifiers first, for the sake of the item layer pixels
        if (tool != null && !modifierModels.isEmpty()) {
            addModifierQuads(spriteGetter, modifierModels, firstModifiers, tool, smallQuads::add, smallPixels, smallTransforms, false);
            // if we have a large model, that means we will fetch models twice, once for large then again for small
            if (largeTransforms != null) {
                addModifierQuads(spriteGetter, modifierModels, firstModifiers, tool, largeQuads::add, largePixels, largeTransforms, true);
            }
        }

        // add quads for all parts
        Sprite particle = null;
        for (int i = parts.size() - 1; i >= 0; i--) {
            ToolPart part = parts.get(i);

            // part with materials
            if (part.hasMaterials()) {
                // start by fetching the material we are rendering at this position, should only be null on invalid tools or during the initial bake
                int index = part.index();
                MaterialVariantId material = index < materials.size() ? materials.get(index) : IMaterial.UNKNOWN_ID;
                TintedSprite materialSprite = MaterialModel.getMaterialSprite(spriteGetter, owner.getMaterial(part.getName(false)), material);
                particle = materialSprite.sprite();

                // need full quads for both as small is directly rendered in a few non-GUI cases
                smallQuads.add(MantleItemLayerModel.getQuadsForSprite(materialSprite.color(), -1, materialSprite.sprite(), smallTransforms, materialSprite.emissivity(), smallPixels));
                if (largeTransforms != null) {
                    largeQuads.add(MaterialModel.getQuadsForMaterial(spriteGetter, owner.getMaterial(part.getName(true)), material, -1, largeTransforms, largePixels));
                }
            } else {
                // part without materials
                particle = spriteGetter.apply(owner.getMaterial(part.getName(false)));
                // same drill as above
                smallQuads.add(MantleItemLayerModel.getQuadsForSprite(-1, -1, particle, smallTransforms, 0, smallPixels));
                if (largeTransforms != null) {
                    largeQuads.add(MantleItemLayerModel.getQuadsForSprite(-1, -1, spriteGetter.apply(owner.getMaterial(part.getName(true))), largeTransforms, 0, largePixels));
                }
            }
        }
        // should never happen, but just in case prevents a NPE
        if (particle == null) {
            particle = spriteGetter.apply(new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, MissingSprite.getMissingSpriteId()));
            TConstruct.LOG.error("Created tool model without a particle sprite, this means it somehow has no parts. This should not be possible");
        }

        // start by building the small models, one for GUI and one outside
        IModelBuilder<?> smallModelBuilder = makeModelBuilder(owner, overrides, particle);
        IModelBuilder<?> guiModelBuilder = makeModelBuilder(owner, overrides, particle);
        smallQuads.build(quads -> quads.forEach(quad -> {
            smallModelBuilder.addUnculledFace(quad);
            if (quad.getFace() == Direction.SOUTH) {
                guiModelBuilder.addUnculledFace(quad);
            }
        }));
        if (largeTransforms == null) {
            return new BakedUniqueGuiModel(smallModelBuilder.build(), guiModelBuilder.build());
        }
        IModelBuilder<?> largeModelBuilder = makeModelBuilder(owner, overrides, particle);
        largeQuads.build(quads -> quads.forEach(largeModelBuilder::addUnculledFace));
        return new BakedLargeToolModel(largeModelBuilder.build(), smallModelBuilder.build(), guiModelBuilder.build());
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, ModelOverrideList overrides, Identifier modelLocation, boolean b) {
        AffineTransformation largeTransforms = isLarge ? new AffineTransformation(new Vector3f((offset.x - 8) / 32, (-offset.y - 8) / 32, 0), null, new Vector3f(2, 2, 1), null) : null;
        overrides = new MaterialOverrideHandler(owner, toolParts, firstModifiers, largeTransforms, modifierModels, overrides);
        // bake the original with no tool, meaning it will skip modifiers and materials
        return bakeInternal(owner, spriteGetter, largeTransforms, toolParts, modifierModels, firstModifiers, Collections.emptyList(), null, overrides);
    }

    /**
     * Swaps out the large model for the small or gui model as needed
     */
    private static class BakedLargeToolModel extends BakedModelWrapper<BakedModel> {
        private final BakedModel small;
        private final BakedModel gui;

        public BakedLargeToolModel(BakedModel large, BakedModel small, BakedModel gui) {
            super(large);
            this.small = small;
            this.gui = gui;
        }

        @Override
        public BakedModel applyTransform(TransformType cameraTransformType, MatrixStack mat, boolean applyLeftHandTransform) {
            BakedModel model = originalModel;
            if (cameraTransformType == TransformType.GUI) {
                model = gui;
            } else if (SMALL_TOOL_TYPES.get(cameraTransformType.ordinal())) {
                model = small;
            }
            return model.applyTransform(cameraTransformType, mat, applyLeftHandTransform);
        }
    }

    /**
     * Data class for a single tool part
     */
    private record ToolPart(String name, int index) {
        /**
         * Default tool part instance for breakable textures
         */
        public static final ToolPart DEFAULT = new ToolPart("tool", -1);
        /**
         * Default tool part list if one is not defined
         */
        public static final List<ToolPart> DEFAULT_PARTS = List.of(DEFAULT);

        /**
         * If true, this part has material variants
         */
        public boolean hasMaterials() {
            return index >= 0;
        }

        /**
         * Gets the name for this part
         *
         * @param isLarge If true, rendering a large tool
         * @return Texture name for this part
         */
        public String getName(boolean isLarge) {
            if (isLarge) {
                return "large_" + name;
            }
            return name;
        }

        /**
         * Reads a part from JSON
         */
        public static ToolPart read(JsonObject json) {
            String name = net.minecraft.util.JsonHelper.getString(json, "name");
            int index = net.minecraft.util.JsonHelper.getInt(json, "index", -1);
            return new ToolPart(name, index);
        }
    }

    /**
     * Dynamic override handler to swap in the material texture
     */
    public static final class MaterialOverrideHandler extends ModelOverrideList {
        /**
         * If true, we are currently resolving a nested model and should ignore further nesting
         */
        private static boolean ignoreNested = false;

        // contains all the baked models since they'll never change, cleared automatically as the baked model is discarded
        private final Cache<ToolCacheKey, BakedModel> cache = CacheBuilder
                .newBuilder()
                // ensure we can display every single tool that shows in JEI, plus a couple extra
                .maximumSize(MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().size() * 3L / 2)
                .build();

        // parameters needed for rebaking
        private final JsonUnbakedModel owner;
        private final List<ToolPart> toolParts;
        private final List<FirstModifier> firstModifiers;
        @Nullable
        private final AffineTransformation largeTransforms;
        private final Map<ModifierId, IBakedModifierModel> modifierModels;
        private final ModelOverrideList nested;

        private MaterialOverrideHandler(JsonUnbakedModel owner, List<ToolPart> toolParts, List<FirstModifier> firstModifiers, @Nullable AffineTransformation largeTransforms, Map<ModifierId, IBakedModifierModel> modifierModels, ModelOverrideList nested) {
            this.owner = owner;
            this.toolParts = toolParts;
            this.firstModifiers = firstModifiers;
            this.largeTransforms = largeTransforms;
            this.modifierModels = modifierModels;
            this.nested = nested;
        }

        /**
         * Bakes a copy of this model using the given material
         *
         * @param materials New materials for the model
         * @return Baked model
         */
        private BakedModel bakeDynamic(List<MaterialVariantId> materials, IToolStackView tool) {
            // bake internal does not require an instance to bake, we can pass in whatever material we want
            // use empty override list as the sub model never calls overrides, and already has a material
            return bakeInternal(owner, SpriteIdentifier::getSprite, largeTransforms, toolParts, modifierModels, firstModifiers, materials, tool, ModelOverrideList.EMPTY);
        }

        @Override
        public BakedModel apply(BakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed) {
            // first, resolve the overrides
            // hack: we set a boolean flag to prevent that model from resolving its nested overrides, no nesting multiple deep
            if (!ignoreNested) {
                BakedModel overridden = nested.apply(originalModel, stack, world, entity, seed);
                if (overridden != null && overridden != originalModel) {
                    ignoreNested = true;
                    // if the override does have a new model, make sure to fetch its overrides to handle the nested texture as its most likely a tool model
                    BakedModel finalModel = overridden.getOverrides().apply(overridden, stack, world, entity, seed);
                    ignoreNested = false;
                    return finalModel;
                }
            }
            // use material IDs for the sake of internal rendering materials
            List<MaterialVariantId> materialIds = MaterialIdNBT.from(stack).getMaterials();
            IToolStackView tool = ToolStack.from(stack);

            // if nothing unique, render original
            skip:
            if (materialIds.isEmpty() && tool.getUpgrades().isEmpty()) {
                for (FirstModifier modifier : firstModifiers) {
                    if (modifier.forced) {
                        break skip;
                    }
                }
                return originalModel;
            }

            // build the cache key for the modifiers, based on what the modifier requests
            // for many, it is just the modifier entry, but they can have more complex keys if needed
            ImmutableList.Builder<Object> builder = ImmutableList.builder();
            Set<ModifierId> hidden = ModifierSetWorktableRecipe.getModifierSet(tool.getPersistentData(), TConstruct.getResource("invisible_modifiers"));
            ModifierEntry[] firstEntries = new ModifierEntry[firstModifiers.size()];
            for (ModifierEntry entry : tool.getUpgrades().getModifiers()) {
                ModifierId id = entry.getId();
                int index = FirstModifier.indexOf(firstModifiers, id);
                if (index != -1) {
                    // handle all the first entries together, keeps their order consistent
                    firstEntries[index] = entry;
                } else if (!hidden.contains(id)) {
                    IBakedModifierModel model = modifierModels.get(id);
                    if (model != null) {
                        Object cacheKey = model.getCacheKey(tool, entry);
                        if (cacheKey != null) {
                            builder.add(cacheKey);
                        }
                    }
                }
            }
            for (int i = 0; i < firstModifiers.size(); i++) {
                FirstModifier modifier = firstModifiers.get(i);
                ModifierEntry entry = firstEntries[i];
                if (entry != null || modifier.forced) {
                    IBakedModifierModel model = modifierModels.get(modifier.id);
                    if (model != null) {
                        Object cacheKey = model.getCacheKey(tool, Objects.requireNonNullElse(entry, ModifierEntry.EMPTY));
                        if (cacheKey != null) {
                            builder.add(cacheKey);
                        }
                    }
                }
            }

            // render special model
            try {
                return cache.get(new ToolCacheKey(materialIds, builder.build()), () -> bakeDynamic(materialIds, tool));
            } catch (ExecutionException e) {
                TConstruct.LOG.error("Failed to get tool model from cache", e);
                return originalModel;
            }
        }
    }

    /**
     * Simple data class to cache built tool modifiers, contains everything unique in the textures
     */
    private record ToolCacheKey(List<MaterialVariantId> materials, List<Object> modifierData) {
    }
}
