package slimeknights.tconstruct.world.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SkullBlock.SkullType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;

import java.util.Map;

/**
 * Generics do not match to use the vanilla armor layer, so this is a reimplementation of some of {@link ArmorFeatureRenderer}
 */
public class SlimeArmorLayer<T extends SlimeEntity, M extends SinglePartEntityModel<T>, A extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    private final A armorModel;
    public final Map<SkullType, SkullBlockEntityModel> skullModels;
    private final boolean lavaSlime;

    public SlimeArmorLayer(FeatureRendererContext<T, M> pRenderer, A armorModel, EntityModelLoader modelSet, boolean lavaSlime) {
        super(pRenderer);
        this.armorModel = armorModel;
        this.skullModels = SkullBlockEntityRenderer.getModels(modelSet);
        this.lavaSlime = lavaSlime;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider buffer, int packedLight, T entity, float pLimbSwing, float swing, float partialTicks, float age, float headYaw, float headPitch) {
        ItemStack helmet = entity.getEquippedStack(EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            matrices.push();
            if (this.lavaSlime) {
                float squish = MathHelper.lerp(partialTicks, entity.lastStretch, entity.stretch);
                if (squish < 0) {
                    squish = 0;
                }
                matrices.translate(0, 1.5 - 0.425 * squish, 0);
            } else {
                matrices.translate(0, 1.5, 0);
            }
            matrices.scale(0.9f, 0.9f, 0.9f);

            Item item = helmet.getItem();
            // helmet renderer, based on humanoid armor layer
            if (item instanceof ArmorItem armor && armor.getSlot() == EquipmentSlot.HEAD) {
                this.getContextModel().copyStateTo(this.armorModel);
                this.armorModel.setVisible(false);
                this.armorModel.head.visible = true;
                this.armorModel.hat.visible = true;
                //noinspection UnstableApiUsage  I am reimplementing vanilla stuff, I will call vanilla hooks
                Model model = ForgeHooksClient.getArmorModel(entity, helmet, EquipmentSlot.HEAD, this.armorModel);
                boolean enchanted = helmet.hasGlint();
                if (armor instanceof DyeableItem dyeable) {
                    int color = dyeable.getColor(helmet);
                    float red = (color >> 16 & 255) / 255.0F;
                    float green = (color >> 8 & 255) / 255.0F;
                    float blue = (color & 255) / 255.0F;
                    renderModel(matrices, buffer, packedLight, enchanted, model, red, green, blue, getArmorResource(entity, helmet, armor, ""));
                    renderModel(matrices, buffer, packedLight, enchanted, model, 1.0F, 1.0F, 1.0F, getArmorResource(entity, helmet, armor, "_overlay"));
                } else {
                    renderModel(matrices, buffer, packedLight, enchanted, model, 1.0F, 1.0F, 1.0F, getArmorResource(entity, helmet, armor, ""));
                }
            } else {
                // block model renderer, based on custom head layer

                // skull block rendering
                if (item instanceof BlockItem block && block.getBlock() instanceof AbstractSkullBlock skullBlock) {
                    matrices.scale(1.1875F, -1.1875F, -1.1875F);
                    GameProfile gameprofile = null;
                    NbtCompound tag = helmet.getNbt();
                    if (tag != null && tag.contains("SkullOwner", NbtElement.COMPOUND_TYPE)) {
                        gameprofile = NbtHelper.toGameProfile(tag.getCompound("SkullOwner"));
                    }
                    matrices.translate(-0.5, 0.0, -0.5);
                    SkullBlock.SkullType type = skullBlock.getSkullType();
                    SkullBlockEntityModel skullModel = this.skullModels.get(type);
                    RenderLayer renderType = SkullBlockEntityRenderer.getRenderLayer(type, gameprofile);
                    SkullBlockEntityRenderer.renderSkull(null, 180.0F, pLimbSwing, matrices, buffer, packedLight, skullModel, renderType);
                } else {
                    // standard rendering
                    HeadFeatureRenderer.translate(matrices, false);
                    MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer().renderItem(entity, helmet, ItemTransforms.TransformType.HEAD, false, matrices, buffer, packedLight);
                }
            }
            matrices.pop();
        }
    }

    private static void renderModel(MatrixStack matrices, VertexConsumerProvider buffer, int packedLight, boolean enchanted, Model model, float red, float green, float blue, Identifier texture) {
        VertexConsumer vertexconsumer = ItemRenderer.getArmorGlintConsumer(buffer, RenderLayer.getArmorCutoutNoCull(texture), false, enchanted);
        model.render(matrices, vertexconsumer, packedLight, OverlayTexture.DEFAULT_UV, red, green, blue, 1.0F);
    }

    /**
     * More generic ForgeHook version of the above function, it allows for Items to have more control over what texture they provide.
     *
     * @param entity Entity wearing the armor
     * @param stack  ItemStack for the armor
     * @param armor  Armor item instance
     * @param type   Subtype, can be null or "overlay"
     * @return ResourceLocation pointing at the armor's texture
     */
    public static Identifier getArmorResource(Entity entity, ItemStack stack, ArmorItem armor, String type) {
        String texture = armor.getMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String path = String.format(java.util.Locale.ROOT, "%s:textures/models/armor/%s_layer_1%s.png", domain, texture, type);
        path = ForgeHooksClient.getArmorTexture(entity, stack, path, EquipmentSlot.HEAD, type);
        Identifier location = ArmorFeatureRenderer.ARMOR_TEXTURE_CACHE.get(path);
        if (location == null) {
            location = new Identifier(path);
            ArmorFeatureRenderer.ARMOR_TEXTURE_CACHE.put(path, location);
        }

        return location;
    }
}
