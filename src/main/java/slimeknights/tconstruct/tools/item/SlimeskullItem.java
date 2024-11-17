package slimeknights.tconstruct.tools.item;

import com.iafenvoy.uranus.client.render.armor.IArmorTextureProvider;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;

import java.util.function.Consumer;

/**
 * This item is mainly to return the proper model for a slimeskull
 */
public class SlimeskullItem extends ModifiableArmorItem implements IArmorTextureProvider {
    private final Identifier name;

    public SlimeskullItem(ModifiableArmorMaterial material, Settings properties) {
        super(material, ArmorSlotType.HELMET, properties);
        this.name = material.getId();
    }

    @Override
    public Identifier getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return new Identifier(TConstruct.MOD_ID, ArmorUtil.getDummyArmorTexture(slot));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new ArmorModelDispatcher() {
            @Override
            protected Identifier getName() {
                return SlimeskullItem.this.name;
            }

            @NotNull
            @Override
            public Model getGenericArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, BipedEntityModel<?> original) {
                return SlimeskullArmorModel.INSTANCE.setup(living, stack, original, this.getModel(stack));
            }
        });
    }
}
