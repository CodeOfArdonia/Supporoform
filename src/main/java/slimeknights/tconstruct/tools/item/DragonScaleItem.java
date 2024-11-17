package slimeknights.tconstruct.tools.item;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import slimeknights.mantle.item.TooltipItem;

/**
 * Explosion immune tooltip item
 */
public class DragonScaleItem extends TooltipItem {
    public DragonScaleItem(Settings properties) {
        super(properties);
    }

    @Override
    public boolean damage(DamageSource damageSource) {
        return !damageSource.isIn(DamageTypeTags.IS_EXPLOSION);
    }
}
