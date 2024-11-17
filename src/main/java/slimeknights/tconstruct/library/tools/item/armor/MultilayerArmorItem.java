package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;
import slimeknights.tconstruct.tools.item.ArmorSlotType;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Armor model that applies multiple texture layers in order
 */
public class MultilayerArmorItem extends ModifiableArmorItem {
    private final Identifier name;

    public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorSlotType slot, Settings properties) {
        super(material, slot, properties);
        this.name = material.getId();
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return ArmorUtil.getDummyArmorTexture(slot);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new ArmorModelDispatcher() {
            @Override
            protected Identifier getName() {
                return MultilayerArmorItem.this.name;
            }
        });
    }
}
