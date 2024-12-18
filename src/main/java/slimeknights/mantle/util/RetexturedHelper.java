package slimeknights.mantle.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * This utility contains helpers to handle the NBT for retexturable blocks
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RetexturedHelper {
    /**
     * Tag name for texture blocks. Should not be used directly, use the utils to interact
     */
    public static final String TAG_TEXTURE = "texture";
    /**
     * Property for tile entities containing a texture block
     */
    public static final ModelProperty<Block> BLOCK_PROPERTY = new ModelProperty<>(block -> block != Blocks.AIR);


    /* Texture name */

    /**
     * Gets the name of the texture from NBT
     *
     * @param nbt NBT tag
     * @return Name of the texture, or empty if no texture
     */
    public static String getTextureName(@Nullable NbtCompound nbt) {
        if (nbt == null) {
            return "";
        }
        return nbt.getString(TAG_TEXTURE);
    }

    /**
     * Gets the texture name from a stack
     *
     * @param stack Stack
     * @return Texture, or empty string if none
     */
    public static String getTextureName(ItemStack stack) {
        return getTextureName(stack.getNbt());
    }

    /**
     * Gets the name of the texture from the block
     *
     * @param block Block
     * @return Name of the texture, or empty if the block is air
     */
    public static String getTextureName(Block block) {
        if (block == Blocks.AIR) {
            return "";
        }
        return Objects.requireNonNull(Registries.BLOCK.getId(block)).toString();
    }


    /* Texture */

    /**
     * Gets a block for the given name
     *
     * @param name Block name
     * @return Block entry, or {@link Blocks#AIR} if no match
     */
    public static Block getBlock(String name) {
        if (!name.isEmpty()) {
            return Registries.BLOCK.get(new Identifier(name));
        }
        return Blocks.AIR;
    }

    /**
     * Gets the texture from a stack
     *
     * @param stack Stack to fetch texture
     * @return Texture, or {@link Blocks#AIR} if none
     */
    public static Block getTexture(ItemStack stack) {
        return getBlock(getTextureName(stack));
    }


    /* Setting */

    /**
     * Sets the texture in an NBT instance
     *
     * @param nbt     Tag instance
     * @param texture Texture to set
     */
    public static void setTexture(@Nullable NbtCompound nbt, String texture) {
        if (nbt != null) {
            if (texture.isEmpty()) {
                nbt.remove(TAG_TEXTURE);
            } else {
                nbt.putString(TAG_TEXTURE, texture);
            }
        }
    }

    /**
     * Creates a new item stack with the given block as it's texture tag
     *
     * @param stack Stack to modify
     * @param name  Block name to set. If empty, clears the tag
     * @return The item stack with the proper NBT
     */
    public static ItemStack setTexture(ItemStack stack, String name) {
        if (!name.isEmpty()) {
            setTexture(stack.getOrCreateNbt(), name);
        } else if (stack.hasNbt()) {
            setTexture(stack.getNbt(), name);
        }
        return stack;
    }

    /**
     * Creates a new item stack with the given block as it's texture tag
     *
     * @param stack Stack to modify
     * @param block Block to set
     * @return The item stack with the proper NBT
     */
    public static ItemStack setTexture(ItemStack stack, @Nullable Block block) {
        if (block == null || block == Blocks.AIR) {
            return setTexture(stack, "");
        }
        return setTexture(stack, Registries.BLOCK.getId(block).toString());
    }


    /* Block entity */

    /**
     * Helper to call client side when the model data changes to refresh model data
     */
    public static void onTextureUpdated(BlockEntity self) {
        // update the texture in BE data
        World level = self.getWorld();
        if (level != null && level.isClient) {
            self.requestModelDataUpdate();
            BlockState state = self.getCachedState();
            level.updateListeners(self.getPos(), state, state, 0);
        }
    }

    /**
     * Creates a builder with the block property as specified
     */
    public static ModelData.Builder getModelDataBuilder(Block block) {
        // cannot support air, saves a conditional on usage
        if (block == Blocks.AIR) {
            block = null;
        }
        return ModelData.builder().with(BLOCK_PROPERTY, block);
    }

    /**
     * Creates model data with the block property as specified
     */
    public static ModelData getModelData(Block block) {
        return getModelDataBuilder(block).build();
    }


    /* Block */

    /**
     * Adds the texture block to the tooltip
     *
     * @param stack   Stack instance
     * @param tooltip Tooltip
     */
    public static void addTooltip(ItemStack stack, List<Text> tooltip) {
        Block block = getTexture(stack);
        if (block != Blocks.AIR) {
            tooltip.add(block.getName());
        }
    }

    /**
     * Adds all blocks from the block tag to the specified block for fillItemGroup
     *
     * @param block           Dynamic texture item instance
     * @param tag             Tag for texturing
     * @param list            List of texture blocks
     * @param showAllVariants If true, shows all variants. If false, shows just the first
     */
    public static void addTagVariants(ItemConvertible block, TagKey<Item> tag, DefaultedList<ItemStack> list, boolean showAllVariants) {
        boolean added = false;
        // using item tags as that is what will be present in the recipe
        for (RegistryEntry<Item> candidate : Registries.ITEM.iterateEntries(tag)) {
            if (!candidate.hasKeyAndValue()) {
                continue;
            }
            Item item = candidate.value();
            // Don't add instances of the block itself, see Inspirations enlightened bushes
            if (item == block.asItem()) {
                continue;
            }
            // non-block items don't have the textures we need
            if (!(item instanceof BlockItem blockItem)) {
                continue;
            }
            added = true;
            list.add(RetexturedHelper.setTexture(new ItemStack(block), blockItem.getBlock()));
            if (!showAllVariants) {
                return;
            }
        }
        // if we never got one, just add the textureless one
        if (!added) {
            list.add(new ItemStack(block));
        }
    }
}
