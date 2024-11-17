package slimeknights.tconstruct.library.client.model.block;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import lombok.AllArgsConstructor;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ExtraTextureContext;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.model.BakedUniqueGuiModel;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.smeltery.item.TankItem;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * This model contains a single scalable fluid that can either be statically rendered or rendered in the TESR. It also supports rendering fluids in the item model
 */
@AllArgsConstructor
public class TankModel implements IUnbakedGeometry<TankModel> {
    protected static final Identifier BAKE_LOCATION = TConstruct.getResource("dynamic_model_baking");

    /**
     * Shared loader instance
     */
    public static final IGeometryLoader<TankModel> LOADER = TankModel::deserialize;

    protected final SimpleBlockModel model;
    @Nullable
    protected final SimpleBlockModel gui;
    protected final IncrementalFluidCuboid fluid;
    protected final boolean forceModelFluid;

    @Override
    public Collection<SpriteIdentifier> getMaterials(JsonUnbakedModel owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        Collection<SpriteIdentifier> textures = new HashSet<>(model.getMaterials(owner, modelGetter, missingTextureErrors));
        if (gui != null) {
            textures.addAll(gui.getMaterials(owner, modelGetter, missingTextureErrors));
        }
        return textures;
    }

    @Override
    public BakedModel bake(JsonUnbakedModel owner, Baker bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings transform, ModelOverrideList overrides, Identifier location, boolean b) {
        BakedModel baked = model.bake(owner, bakery, spriteGetter, transform, overrides, location);
        // bake the GUI model if present
        BakedModel bakedGui = baked;
        if (gui != null) {
            bakedGui = gui.bake(owner, bakery, spriteGetter, transform, overrides, location);
        }
        return new Baked<>(owner, transform, baked, bakedGui, this);
    }

    /**
     * Override to add the fluid part to the item model
     */
    private static class FluidPartOverride extends ModelOverrideList {
        /**
         * Shared override instance, since the logic is not model dependent
         */
        public static final FluidPartOverride INSTANCE = new FluidPartOverride();

        @Override
        public BakedModel apply(BakedModel model, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed) {
            // ensure we have a fluid
            if (stack.isEmpty() || !stack.hasNbt()) {
                return model;
            }
            // determine fluid
            FluidTank tank = TankItem.getFluidTank(stack);
            if (tank.isEmpty()) {
                return model;
            }
            // always baked model as this override is only used in our model
            return ((Baked<?>) model).getCachedModel(tank.getFluid(), tank.getCapacity());
        }
    }

    /**
     * Baked variant to load in the custom overrides
     *
     * @param <T> Parent model type, used to make this easier to extend
     */
    public static class Baked<T extends TankModel> extends BakedUniqueGuiModel {
        private final JsonUnbakedModel owner;
        private final ModelBakeSettings originalTransforms;
        @SuppressWarnings("WeakerAccess")
        protected final T original;
        private final Cache<FluidStack, BakedModel> cache = CacheBuilder
                .newBuilder()
                .maximumSize(64)
                .build();

        @SuppressWarnings("WeakerAccess")
        protected Baked(JsonUnbakedModel owner, ModelBakeSettings transforms, BakedModel baked, BakedModel gui, T original) {
            super(baked, gui);
            this.owner = owner;
            this.originalTransforms = transforms;
            this.original = original;
        }

        @Override
        public ModelOverrideList getOverrides() {
            return FluidPartOverride.INSTANCE;
        }

        /**
         * Bakes the model with the given fluid element
         *
         * @param owner      Owner for baking, should include the fluid texture
         * @param baseModel  Base model for original elements
         * @param fluid      Fluid element for baking
         * @param color      Color for the fluid part
         * @param luminosity Luminosity for the fluid part
         * @return Baked model
         */
        private BakedModel bakeWithFluid(JsonUnbakedModel owner, SimpleBlockModel baseModel, ModelElement fluid, int color, int luminosity) {
            // setup for baking, using dynamic location and sprite getter
            Function<SpriteIdentifier, Sprite> spriteGetter = SpriteIdentifier::getSprite;
            Sprite particle = spriteGetter.apply(owner.resolveSprite("particle"));
            BasicBakedModel.Builder builder = SimpleBlockModel.bakedBuilder(owner, ModelOverrideList.EMPTY).setParticle(particle);
            RenderContext.QuadTransform quadTransformer = SimpleBlockModel.applyTransform(originalTransforms, owner.getRootTransform());
            // first, add all regular elements
            for (ModelElement element : baseModel.getElements()) {
                SimpleBlockModel.bakePart(builder, owner, element, spriteGetter, originalTransforms, quadTransformer, BAKE_LOCATION);
            }
            // next, add in the fluid
            RenderContext.QuadTransform fluidTransformer = color == -1 ? quadTransformer : quadTransformer.transform(ColoredBlockModel.applyColorQuadTransformer(color));
            ColoredBlockModel.bakePart(builder, owner, fluid, luminosity, spriteGetter, originalTransforms.getRotation(), fluidTransformer, originalTransforms.isUvLocked(), BAKE_LOCATION);
            return builder.build(SimpleBlockModel.getRenderTypeGroup(owner));
        }

        /**
         * Gets the model with the fluid part added
         *
         * @param stack Fluid stack to add
         * @return Model with the fluid part
         */
        private BakedModel getModel(FluidStack stack) {
            // fetch fluid data
            IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(stack.getFluid());
            FluidType type = stack.getFluid().getFluidType();
            int color = attributes.getTintColor(stack);
            int luminosity = type.getLightLevel(stack);
            Map<String, SpriteIdentifier> textures = ImmutableMap.of(
                    "fluid", new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, attributes.getStillTexture(stack)),
                    "flowing_fluid", new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, attributes.getFlowingTexture(stack)));
            JsonUnbakedModel textured = new ExtraTextureContext(owner, textures);

            // add fluid part
            ModelElement fluid = original.fluid.getPart(stack.getAmount(), type.isLighterThanAir());
            // bake the model
            BakedModel baked = bakeWithFluid(textured, original.model, fluid, color, luminosity);

            // if we have GUI, bake a GUI variant
            if (original.gui != null) {
                baked = new BakedUniqueGuiModel(baked, bakeWithFluid(textured, original.gui, fluid, color, 0));
            }

            // return what we ended up with
            return baked;
        }

        /**
         * Gets a cached model with the fluid part added
         *
         * @param fluid Scaled contained fluid
         * @return Cached model
         */
        private BakedModel getCachedModel(FluidStack fluid) {
            try {
                return cache.get(fluid, () -> getModel(fluid));
            } catch (ExecutionException e) {
                TConstruct.LOG.error(e);
                return this;
            }
        }

        /**
         * Gets a cached model with the fluid part added
         *
         * @param fluid    Fluid contained
         * @param capacity Tank capacity
         * @return Cached model
         */
        private BakedModel getCachedModel(FluidStack fluid, int capacity) {
            int increments = original.fluid.getIncrements();
            return getCachedModel(new FluidStack(fluid, MathHelper.clamp(fluid.getAmount() * increments / capacity, 1, increments)));
        }

        @NotNull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, ModelData data, @Nullable RenderLayer renderType) {
            if ((original.forceModelFluid || Config.CLIENT.tankFluidModel.get()) && data.has(ModelProperties.FLUID_TANK)) {
                IFluidTank tank = data.get(ModelProperties.FLUID_TANK);
                if (tank != null && !tank.getFluid().isEmpty()) {
                    return getCachedModel(tank.getFluid(), tank.getCapacity()).getQuads(state, side, rand, ModelData.EMPTY, renderType);
                }
            }
            return originalModel.getQuads(state, side, rand, data, renderType);
        }

        /**
         * Gets the fluid location
         *
         * @return Fluid location data
         */
        public IncrementalFluidCuboid getFluid() {
            return original.fluid;
        }
    }

    /**
     * Loader for this model
     */
    public static TankModel deserialize(JsonObject json, JsonDeserializationContext context) {
        SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);
        SimpleBlockModel gui = null;
        if (json.has("gui")) {
            gui = SimpleBlockModel.deserialize(JsonHelper.getObject(json, "gui"), context);
        }
        IncrementalFluidCuboid fluid = IncrementalFluidCuboid.fromJson(JsonHelper.getObject(json, "fluid"));
        boolean forceModelFluid = JsonHelper.getBoolean(json, "render_fluid_in_model", false);
        return new TankModel(model, gui, fluid, forceModelFluid);
    }
}
