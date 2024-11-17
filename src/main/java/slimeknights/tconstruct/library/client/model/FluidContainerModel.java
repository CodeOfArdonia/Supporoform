/*
 * Minecraft Forge
 * Copyright (c) 2016-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package slimeknights.tconstruct.library.client.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Quaternion;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.models.CompositeModel;
import io.github.fabricators_of_create.porting_lib.models.DynamicFluidContainerModel;
import io.github.fabricators_of_create.porting_lib.models.QuadTransformers;
import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.models.geometry.SimpleModelState;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.StandaloneGeometryBakingContext;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;

import java.util.*;
import java.util.function.Function;

/**
 * Extension of {@link net.minecraftforge.client.model.DynamicFluidContainerModel} with two additional features: baked tints and fluid stack sensitive models.
 * Does not handle covers as I have never seen a need for them, and it means less code duplication (plus the forge model does the whole cover is mask thing wrong compared to 1.18).
 */
public record FluidContainerModel(FluidStack fluid, boolean flipGas) implements IUnbakedGeometry<FluidContainerModel> {
    public static final IGeometryLoader<FluidContainerModel> LOADER = FluidContainerModel::deserialize;

    /**
     * Clone of same named field from {@link net.minecraftforge.client.model.DynamicFluidContainerModel}
     */
    public static final AffineTransformation FLUID_TRANSFORM = new AffineTransformation(new Vector3f(0, 0, 0), Quaternionf.ONE, new Vector3f(1, 1, 1.002f), Quaternion.ONE);

    /**
     * Deserializes this model from JSON
     */
    public static FluidContainerModel deserialize(JsonObject json, JsonDeserializationContext context) {
        FluidStack fluidStack = FluidStack.EMPTY;
        // parse the fluid with an optional tag
        if (json.has("fluid")) {
            JsonElement fluidElement = json.get("fluid");
            Fluid fluid;
            NbtCompound tag = null;
            if (fluidElement.isJsonObject()) {
                JsonObject fluidObject = fluidElement.getAsJsonObject();
                fluid = JsonHelper.getAsEntry(Registries.FLUID, fluidObject, "name");
                if (fluidObject.has("nbt")) {
                    tag = CraftingHelper.getNBT(fluidObject.get("nbt"));
                }
            } else {
                fluid = JsonHelper.convertToEntry(Registries.FLUID, fluidElement, "fluid");
            }
            fluidStack = new FluidStack(fluid, FluidType.BUCKET_VOLUME, tag);
        }
        boolean flipGas = net.minecraft.util.JsonHelper.getBoolean(json, "flip_gas", true);
        return new FluidContainerModel(fluidStack, flipGas);
    }

    /**
     * Adds a material to the set if its defined
     */
    private static void addMaterial(Set<SpriteIdentifier> textures, JsonUnbakedModel owner, String key) {
        if (owner.hasMaterial(key)) {
            textures.add(owner.getMaterial(key));
        }
    }

    @Override
    public Collection<SpriteIdentifier> getMaterials(JsonUnbakedModel owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        Set<SpriteIdentifier> textures = Sets.newHashSet();
        addMaterial(textures, owner, "particle");
        addMaterial(textures, owner, "base");
        textures.add(owner.getMaterial("fluid"));
        return textures;
    }

    /**
     * Gets the given sprite, or null if the texture is not present in the model
     */
    @Nullable
    private static Sprite getSprite(JsonUnbakedModel context, Function<SpriteIdentifier, Sprite> spriteGetter, String key) {
        if (context.textureExists(key)) {
            return spriteGetter.apply(context.resolveSprite(key));
        }
        return null;
    }

    private static BakedModel bakeInternal(JsonUnbakedModel context, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelState, ModelOverrideList overrides, Identifier modelLocation, FluidStack fluid, boolean flipGas) {
        // get basic sprites
        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluid.getFluid());
        Sprite baseSprite = getSprite(context, spriteGetter, "base");
        Sprite fluidSprite = !fluid.isEmpty() ? spriteGetter.apply(new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, clientFluid.getStillTexture(fluid))) : null;

        // determine particle
        Sprite particleSprite = getSprite(context, spriteGetter, "particle");
        if (particleSprite == null) particleSprite = fluidSprite;
        if (particleSprite == null) particleSprite = baseSprite;
        if (particleSprite == null) {
            TConstruct.LOG.error("No valid particle sprite for fluid container model, you should supply either 'base' or 'particle'");
            particleSprite = spriteGetter.apply(new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, MissingSprite.getMissingSpriteId()));
        }

        // if its a gas and we flipping, flip it
        if (flipGas && !fluid.isEmpty() && fluid.getFluid().getFluidType().isLighterThanAir()) {
            modelState = new SimpleModelState(modelState.getRotation().multiply(new AffineTransformation(null, new Quaternionf(0, 0, 1, 0), null, null)));
        }

        // start building the mode
        CompositeModel.Baked.Builder modelBuilder = CompositeModel.Baked.builder(context, particleSprite, overrides, context.getTransformations());
        RenderTypeGroup renderTypes = DynamicFluidContainerModel.getLayerRenderTypes(false);

        // add in the base
        if (baseSprite != null) {
            modelBuilder.addQuads(renderTypes, UnbakedGeometryHelper.bakeElements(
                    UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite),
                    $ -> baseSprite, modelState, modelLocation
            ));
        }

        // add in fluid
        if (fluidSprite != null) {
            List<BakedQuad> quads = UnbakedGeometryHelper.bakeElements(
                    UnbakedGeometryHelper.createUnbakedItemMaskElements(1, spriteGetter.apply(context.resolveSprite("fluid"))),
                    $ -> fluidSprite,
                    new SimpleModelState(modelState.getRotation().multiply(FLUID_TRANSFORM), modelState.isUvLocked()),
                    modelLocation
            );

            // apply light
            RenderTypeGroup fluidRenderTypes = renderTypes;
            int light = fluid.getFluid().getFluidType().getLightLevel(fluid);
            if (light > 0) {
                fluidRenderTypes = DynamicFluidContainerModel.getLayerRenderTypes(true);
                QuadTransformers.settingEmissivity(light).processInPlace(quads);
            }
            // apply color
            int color = clientFluid.getTintColor(fluid);
            if (color != -1) {
                ColoredBlockModel.applyColorQuadTransformer(color).processInPlace(quads);
            }
            modelBuilder.addQuads(fluidRenderTypes, quads);
        }
        return modelBuilder.build();
    }

    @Override
    public BakedModel bake(JsonUnbakedModel context, Baker bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelState, ModelOverrideList overrides, Identifier modelLocation, boolean b) {
        // We need to disable GUI 3D and block lighting for this to render properly
        context = StandaloneGeometryBakingContext.builder(context).withGui3d(false).withUseBlockLight(false).build(modelLocation);
        // only do contained fluid if we did not set the fluid in the model properties
        if (fluid.isEmpty()) {
            overrides = new ContainedFluidOverrideHandler(context, overrides, modelState, flipGas);
        }
        return bakeInternal(context, spriteGetter, modelState, overrides, modelLocation, fluid, flipGas);
    }

    /**
     * Handles swapping the model based on the contained fluid
     */
    @RequiredArgsConstructor
    private static final class ContainedFluidOverrideHandler extends ModelOverrideList {
        private static final Identifier BAKE_LOCATION = TConstruct.getResource("copper_can_dynamic");

        private final Map<FluidStack, BakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change

        private final JsonUnbakedModel context;
        private final ModelOverrideList nested;
        private final ModelBakeSettings modelState;
        private final boolean flipGas;


        /**
         * Gets the model directly, for creating the cached models
         */
        private BakedModel getUncahcedModel(FluidStack fluid) {
            return bakeInternal(context, SpriteIdentifier::getSprite, modelState, ModelOverrideList.EMPTY, BAKE_LOCATION, fluid, flipGas);
        }

        @Override
        public BakedModel apply(BakedModel originalModel, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed) {
            BakedModel overriden = nested.apply(originalModel, stack, world, entity, seed);
            if (overriden != originalModel) return overriden;
            Optional<FluidStack> optional = FluidUtil.getFluidContained(stack);
            if (optional.isPresent()) {
                FluidStack fluid = optional.get();
                fluid.setAmount(FluidType.BUCKET_VOLUME); // cache considers amount, so ensure its consistent
                return cache.computeIfAbsent(fluid, this::getUncahcedModel);
            }
            return originalModel;
        }
    }
}
