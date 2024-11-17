package slimeknights.tconstruct.library.client.armor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier.ArmorTexture;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier.TextureType;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Common shared logic for material armor models
 */
public abstract class AbstractArmorModel extends Model {
    /**
     * Base model instance for rendering
     */
    @Nullable
    protected BipedEntityModel<?> base;
    /**
     * If true, applies the enchantment glint to extra layers
     */
    protected boolean hasGlint = false;
    /**
     * If true, uses the legs texture
     */
    protected TextureType textureType = TextureType.ARMOR;

    protected boolean hasWings = false;

    protected AbstractArmorModel() {
        super(RenderLayer::getEntityCutoutNoCull);
    }

    /**
     * Sets up the model given the passed arguments
     */
    protected void setup(LivingEntity living, ItemStack stack, EquipmentSlot slot, BipedEntityModel<?> base) {
        this.base = base;
        this.hasGlint = stack.hasGlint();
        this.textureType = TextureType.fromSlot(slot);
        if (slot == EquipmentSlot.CHEST) {
            this.hasWings = ModifierUtil.checkVolatileFlag(stack, ModifiableArmorItem.ELYTRA);
            if (hasWings) {
                ElytraEntityModel<LivingEntity> wings = getWings();
                wings.setAngles(living, 0, 0, 0, 0, 0);
                copyProperties(base, wings);
            }
        } else {
            hasWings = false;
        }
    }

    /**
     * Renders a colored model
     */
    protected void renderColored(Model model, MatrixStack matrices, VertexConsumer buffer, int packedLightIn, int packedOverlayIn, int color, float red, float green, float blue, float alpha) {
        if (color != -1) {
            alpha *= (float) (color >> 24 & 255) / 255.0F;
            red *= (float) (color >> 16 & 255) / 255.0F;
            green *= (float) (color >> 8 & 255) / 255.0F;
            blue *= (float) (color & 255) / 255.0F;
        }
        model.render(matrices, buffer, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }

    /**
     * Renders a single armor texture
     */
    protected void renderTexture(Model model, MatrixStack matrices, int packedLightIn, int packedOverlayIn, ArmorTexture texture, float red, float green, float blue, float alpha) {
        assert buffer != null;
        VertexConsumer overlayBuffer = ItemRenderer.getArmorGlintConsumer(buffer, getRenderType(texture.path()), false, hasGlint);
        renderColored(model, matrices, overlayBuffer, packedLightIn, packedOverlayIn, texture.color(), red, green, blue, alpha);
    }

    /**
     * Renders the wings layer
     */
    protected void renderWings(MatrixStack matrices, int packedLightIn, int packedOverlayIn, ArmorTexture texture, float red, float green, float blue, float alpha) {
        matrices.push();
        matrices.translate(0.0D, 0.0D, 0.125D);
        renderTexture(getWings(), matrices, packedLightIn, packedOverlayIn, texture, red, green, blue, alpha);
        matrices.pop();
    }


    /* Helpers */

    /**
     * Cache of parsed resource locations, similar to the armor layer one
     */
    public static final Function<String, Identifier> RESOURCE_LOCATION_CACHE = Util.memoize(Identifier::tryParse);

    /**
     * Buffer from the render living event, stored as we lose access to it later
     */
    @Nullable
    public static VertexConsumerProvider buffer;

    /**
     * Initializes the wrapper
     */
    public static void init() {
        // register listeners to set and clear the buffer
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderLivingEvent.Pre.class, event -> buffer = event.getMultiBufferSource());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderLivingEvent.Post.class, event -> buffer = null);
    }

    /**
     * Gets a render type for the given texture
     */
    public static RenderLayer getRenderType(String texture) {
        Identifier location = RESOURCE_LOCATION_CACHE.apply(texture);
        if (location != null) {
            return RenderLayer.getArmorCutoutNoCull(location);
        }
        return RenderLayer.getArmorCutoutNoCull(MissingSprite.getMissingSpriteId());
    }

    /**
     * Wings model to render
     */
    @Nullable
    private static ElytraEntityModel<LivingEntity> wingsModel;

    /**
     * Gets or creates the elytra model
     */
    private ElytraEntityModel<LivingEntity> getWings() {
        if (wingsModel == null) {
            wingsModel = new ElytraEntityModel<>(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(EntityModelLayers.ELYTRA));
        }
        return wingsModel;
    }

    /**
     * Handles the unchecked cast to copy entity model properties
     */
    @SuppressWarnings("unchecked")
    public static <T extends LivingEntity> void copyProperties(EntityModel<T> base, EntityModel<?> other) {
        base.copyStateTo((EntityModel<T>) other);
    }
}
