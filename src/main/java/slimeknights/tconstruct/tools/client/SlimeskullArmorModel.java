package slimeknights.tconstruct.tools.client;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModel;
import slimeknights.tconstruct.library.client.armor.MultilayerArmorModel;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.utils.SimpleCache;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.data.material.MaterialIds;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

/**
 * Model to render a slimeskull helmet with both the helmet and skull
 */
public class SlimeskullArmorModel extends MultilayerArmorModel {
    /**
     * Singleton model instance, all data is passed in via setters
     */
    public static final SlimeskullArmorModel INSTANCE = new SlimeskullArmorModel();
    /**
     * Cache of colors for materials
     */
    private static final SimpleCache<String, Integer> MATERIAL_COLOR_CACHE = new SimpleCache<>(mat ->
            Optional.ofNullable(MaterialVariantId.tryParse(mat))
                    .flatMap(MaterialRenderInfoLoader.INSTANCE::getRenderInfo)
                    .map(MaterialRenderInfo::getVertexColor)
                    .orElse(-1));
    /**
     * Listener to clear caches
     */
    public static final ISafeManagerReloadListener RELOAD_LISTENER = manager -> {
        HEAD_MODELS = null;
        MATERIAL_COLOR_CACHE.clear();
    };

    /**
     * Head to render under the helmet
     */
    @Nullable
    private Identifier headTexture;
    /**
     * Tint color for the head
     */
    private int headColor = -1;
    /**
     * Texture for the head
     */
    @Nullable
    private SkullBlockEntityModel headModel;

    private SlimeskullArmorModel() {
    }

    /**
     * Prepares the model
     */
    public Model setup(LivingEntity living, ItemStack stack, BipedEntityModel<?> base, ArmorModel model) {
        super.setup(living, stack, EquipmentSlot.HEAD, base, model);
        MaterialId materialId = MaterialIdNBT.from(stack).getMaterial(0).getId();
        if (!materialId.equals(IMaterial.UNKNOWN_ID)) {
            SkullBlockEntityModel skull = getHeadModel(materialId);
            Identifier texture = HEAD_TEXTURES.get(materialId);
            if (skull != null && texture != null) {
                this.headModel = skull;
                this.headTexture = texture;
                // determine the color to tint the helmet, will use gold, then embellishment, then enderslime
                String embellishmentMaterial;
                if (ModifierUtil.getModifierLevel(stack, TinkerModifiers.golden.getId()) > 0) {
                    embellishmentMaterial = MaterialIds.gold.toString();
                } else {
                    embellishmentMaterial = ModifierUtil.getPersistentString(stack, TinkerModifiers.embellishment.getId());
                    if (embellishmentMaterial.isEmpty()) {
                        embellishmentMaterial = MaterialIds.enderslime.toString();
                    }
                }
                this.headColor = MATERIAL_COLOR_CACHE.apply(embellishmentMaterial);
                return this;
            }
        }
        this.headTexture = null;
        this.headModel = null;
        this.headColor = -1;
        return this;
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumer vertexBuilder, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (this.base != null && buffer != null) {
            if (this.model != ArmorModel.EMPTY) {
                matrixStackIn.push();
                matrixStackIn.translate(0.0D, this.base.child ? -0.015D : -0.02D, 0.0D);
                matrixStackIn.scale(1.01f, 1.1f, 1.01f);
                super.render(matrixStackIn, vertexBuilder, packedLightIn, packedOverlayIn, red, green, blue, alpha);
                matrixStackIn.pop();
            }
            if (this.headModel != null && this.headTexture != null) {
                VertexConsumer headBuilder = ItemRenderer.getArmorGlintConsumer(buffer, RenderLayer.getEntityCutoutNoCullZOffset(this.headTexture), false, this.hasGlint);
                matrixStackIn.push();
                if (this.base.sneaking) {
                    matrixStackIn.translate(0, this.base.head.pivotY / 16.0F, 0);
                }
                if (this.base.child) {
                    matrixStackIn.scale(0.85F, 0.85F, 0.85F);
                    matrixStackIn.translate(0.0D, 1.0D, 0.0D);
                } else {
                    matrixStackIn.scale(1.115f, 1.115f, 1.115f);
                }
                this.headModel.setHeadRotation(0, this.base.head.yaw * 180f / (float) (Math.PI), this.base.head.pitch * 180f / (float) (Math.PI));
                this.renderColored(this.headModel, matrixStackIn, headBuilder, packedLightIn, packedOverlayIn, this.headColor, red, green, blue, alpha);
                matrixStackIn.pop();
            }
        }
    }


    /* Head models */

    /**
     * Map of all skull factories
     */
    private static final Map<MaterialId, Function<EntityModelLoader, ? extends SkullBlockEntityModel>> HEAD_MODEL_FACTORIES = new HashMap<>();
    /**
     * Map of texture for the skull textures
     */
    private static final Map<MaterialId, Identifier> HEAD_TEXTURES = new HashMap<>();

    /**
     * Registers a head model and texture, using the default skull model
     */
    public static void registerHeadModel(MaterialId materialId, EntityModelLayer headModel, Identifier texture) {
        registerHeadModel(materialId, modelSet -> new SkullEntityModel(modelSet.getModelPart(headModel)), texture);
    }

    /**
     * Registers a head model and texture, using a custom skull model
     */
    public static void registerHeadModel(MaterialId materialId, Function<EntityModelLoader, ? extends SkullBlockEntityModel> headFunction, Identifier texture) {
        if (HEAD_MODEL_FACTORIES.containsKey(materialId)) {
            throw new IllegalArgumentException("Duplicate head model " + materialId);
        }
        HEAD_MODEL_FACTORIES.put(materialId, headFunction);
        HEAD_TEXTURES.put(materialId, texture);
    }

    /**
     * Map of baked head models, if null it is not currently computed
     */
    private static Map<MaterialId, SkullBlockEntityModel> HEAD_MODELS;

    /**
     * Gets the head model for the given material
     */
    @Nullable
    private static SkullBlockEntityModel getHeadModel(MaterialId materialId) {
        if (HEAD_MODELS == null) {
            // vanilla rebakes these a lot, so figure we should at least do it every resource reload
            EntityModelLoader modelSet = MinecraftClient.getInstance().getEntityModelLoader();
            ImmutableMap.Builder<MaterialId, SkullBlockEntityModel> models = ImmutableMap.builder();
            for (Entry<MaterialId, Function<EntityModelLoader, ? extends SkullBlockEntityModel>> entry : HEAD_MODEL_FACTORIES.entrySet()) {
                models.put(entry.getKey(), entry.getValue().apply(modelSet));
            }
            HEAD_MODELS = models.build();
        }
        return HEAD_MODELS.get(materialId);
    }
}
