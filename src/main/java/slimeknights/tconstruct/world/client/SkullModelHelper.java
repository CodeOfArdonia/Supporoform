package slimeknights.tconstruct.world.client;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.model.*;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.TinkerHeadType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helps with creation and registration of skull block models
 */
public class SkullModelHelper {
    /**
     * Map of head type to model layer location for each head type
     */
    public static final Map<TinkerHeadType, EntityModelLayer> HEAD_LAYERS = Arrays.stream(TinkerHeadType.values()).collect(
            Collectors.toMap(Function.identity(), type -> new EntityModelLayer(TConstruct.getResource(type.asString() + "_head"), "main"), (a, b) -> a, () -> new EnumMap<>(TinkerHeadType.class)));

    private SkullModelHelper() {
    }

    /**
     * Injects the extra skulls into the given map
     */
    private static ImmutableMap<SkullBlock.SkullType, SkullBlockEntityModel> inject(EntityModelLoader modelSet, Map<SkullBlock.SkullType, SkullBlockEntityModel> original) {
        ImmutableMap.Builder<SkullBlock.SkullType, SkullBlockEntityModel> builder = ImmutableMap.builder();
        builder.putAll(original);
        HEAD_LAYERS.forEach((type, layer) -> builder.put(type, new SkullEntityModel(modelSet.getModelPart(layer))));
        return builder.build();
    }

    /**
     * Creates a head with the given start and texture size
     */
    public static TexturedModelData createHeadLayer(int headX, int headY, int width, int height) {
        ModelData mesh = new ModelData();
        mesh.getRoot().addChild("head", ModelPartBuilder.create().uv(headX, headY).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), ModelTransform.NONE);
        return TexturedModelData.of(mesh, width, height);
    }

    /**
     * Creates a head with a hat, starting the head at 0,0, hat at the values, and using the given size
     */
    @SuppressWarnings("SameParameterValue")
    public static TexturedModelData createHeadHatLayer(int hatX, int hatY, int width, int height) {
        ModelData mesh = SkullEntityModel.getModelData();
        mesh.getRoot().getChild("head").addChild("hat", ModelPartBuilder.create().uv(hatX, hatY).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.25F)), ModelTransform.NONE);
        return TexturedModelData.of(mesh, width, height);
    }

    /**
     * Creates a layer de
     */
    public static TexturedModelData createPiglinHead() {
        ModelData mesh = new ModelData();
        ModelPartData head = mesh.getRoot().addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F).uv(31, 1).cuboid(-2.0F, -4.0F, -5.0F, 4.0F, 4.0F, 1.0F).uv(2, 4).cuboid(2.0F, -2.0F, -5.0F, 1.0F, 2.0F, 1.0F).uv(2, 0).cuboid(-3.0F, -2.0F, -5.0F, 1.0F, 2.0F, 1.0F), ModelTransform.NONE);
        head.addChild("left_ear", ModelPartBuilder.create().uv(51, 6).cuboid(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F), ModelTransform.of(4.5F, -6.0F, 0.0F, 0.0F, 0.0F, (-(float) Math.PI / 6F)));
        head.addChild("right_ear", ModelPartBuilder.create().uv(39, 6).cuboid(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F), ModelTransform.of(-4.5F, -6.0F, 0.0F, 0.0F, 0.0F, ((float) Math.PI / 6F)));
        return TexturedModelData.of(mesh, 64, 64);
    }
}
