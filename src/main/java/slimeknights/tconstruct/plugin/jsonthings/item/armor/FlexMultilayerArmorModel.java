package slimeknights.tconstruct.plugin.jsonthings.item.armor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Armor model with two texture layers, the base and an overlay
 */
public class FlexMultilayerArmorModel extends FlexModifiableArmorItem {
    private final Identifier name;

    public FlexMultilayerArmorModel(ArmorMaterial material, EquipmentSlot slot, Settings properties, ToolDefinition toolDefinition) {
        super(material, slot, properties, toolDefinition);
        this.name = new Identifier(material.getName());
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
                return name;
            }
        });
    }
}
