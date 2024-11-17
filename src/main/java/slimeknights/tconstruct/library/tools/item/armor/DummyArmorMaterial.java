package slimeknights.tconstruct.library.tools.item.armor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import slimeknights.mantle.registration.object.IdAwareObject;

/**
 * Armor material that returns 0 except for name, since we bypass all the usages
 */
@RequiredArgsConstructor
@Getter
public class DummyArmorMaterial implements ArmorMaterial, IdAwareObject {
    private final Identifier id;
    private final SoundEvent equipSound;

    @Override
    public String getName() {
        return this.id.toString();
    }


    /* Required dummy methods */

    @Override
    public int getDurability(ArmorItem.Type type) {
        return 0;
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return 0;
    }

    @Override
    public int getEnchantability() {
        return 0;
    }

    @Override
    @Deprecated
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    @Deprecated
    public float getToughness() {
        return 0;
    }

    @Override
    @Deprecated
    public float getKnockbackResistance() {
        return 0;
    }
}
