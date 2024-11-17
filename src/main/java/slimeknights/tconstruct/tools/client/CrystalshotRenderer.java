package slimeknights.tconstruct.tools.client;

import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tools.item.CrystalshotItem.CrystalshotEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CrystalshotRenderer extends ProjectileEntityRenderer<CrystalshotEntity> {
    private static final Map<String, Identifier> TEXTURES = new HashMap<>();
    private static final Function<String, Identifier> TEXTURE_GETTER = variant -> TConstruct.getResource("textures/entity/arrow/" + variant + ".png");

    public CrystalshotRenderer(Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CrystalshotEntity arrow) {
        return TEXTURES.computeIfAbsent(arrow.getVariant(), TEXTURE_GETTER);
    }
}
