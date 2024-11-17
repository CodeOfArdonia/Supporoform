package slimeknights.tconstruct.common.registration;

import slimeknights.mantle.registration.deferred.ItemDeferredRegister;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.PartCastItem;

import java.util.function.Supplier;

import net.minecraft.item.Item;

public class ItemDeferredRegisterExtension extends ItemDeferredRegister {
    public ItemDeferredRegisterExtension(String modID) {
        super(modID);
    }

    /**
     * Registers a set of three cast items at once
     *
     * @param name        Base name of cast
     * @param constructor Item constructor
     * @return Object containing casts
     */
    public CastItemObject registerCast(String name, Supplier<? extends Item> constructor) {
        ItemObject<Item> cast = this.register(name + "_cast", constructor);
        ItemObject<Item> sandCast = this.register(name + "_sand_cast", constructor);
        ItemObject<Item> redSandCast = this.register(name + "_red_sand_cast", constructor);
        return new CastItemObject(this.resource(name), cast, sandCast, redSandCast);
    }

    /**
     * Registers a set of three cast items at once
     *
     * @param name  Base name of cast
     * @param props Item properties
     * @return Object containing casts
     */
    public CastItemObject registerCast(String name, Item.Settings props) {
        return this.registerCast(name, () -> new Item(props));
    }

    /**
     * Registers a set of three cast items at once using the part item cast
     *
     * @param item  Part item base for the cast
     * @param props Item properties
     * @return Object containing casts
     */
    public CastItemObject registerCast(ItemObject<? extends IMaterialItem> item, Item.Settings props) {
        return this.registerCast(item.getId().getPath(), () -> new PartCastItem(props, item));
    }
}
