package slimeknights.mantle.registration.object;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

/**
 * Object wrapper containing ingots, nuggets, and blocks
 */
public class MetalItemObject extends ItemObject<Block> {
    private final Supplier<? extends Item> ingot;
    private final Supplier<? extends Item> nugget;
    @Getter
    private final TagKey<Block> blockTag;
    @Getter
    private final TagKey<Item> blockItemTag;
    @Getter
    private final TagKey<Item> ingotTag;
    @Getter
    private final TagKey<Item> nuggetTag;

    public MetalItemObject(String tagName, ItemObject<? extends Block> block, Supplier<? extends Item> ingot, Supplier<? extends Item> nugget) {
        super(block);
        this.ingot = ingot;
        this.nugget = nugget;
        this.blockTag = TagKey.of(RegistryKeys.BLOCK, new Identifier("forge", "storage_blocks/" + tagName));
        this.blockItemTag = getTag("storage_blocks/" + tagName);
        this.ingotTag = getTag("ingots/" + tagName);
        this.nuggetTag = getTag("nuggets/" + tagName);
    }

    /**
     * Gets the ingot for this object
     */
    public Item getIngot() {
        return this.ingot.get();
    }

    /**
     * Gets the ingot for this object
     */
    public Item getNugget() {
        return this.nugget.get();
    }

    /**
     * Creates a tag for a resource
     *
     * @param name Tag name
     * @return Tag
     */
    private static TagKey<Item> getTag(String name) {
        return TagKey.of(RegistryKeys.ITEM, new Identifier("forge", name));
    }
}
