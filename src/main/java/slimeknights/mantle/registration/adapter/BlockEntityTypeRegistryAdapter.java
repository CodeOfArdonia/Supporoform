package slimeknights.mantle.registration.adapter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.datafixers.types.Type;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BlockEntityType.BlockEntityFactory;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.registry.Registry;
import net.minecraft.util.Util;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.registration.object.EnumObject;

import java.util.function.Consumer;

/**
 * Registry adapter for tile entity types with helpers for adding blocks
 */
@SuppressWarnings("unused")
public class BlockEntityTypeRegistryAdapter extends RegistryAdapter<BlockEntityType<?>> {
    /**
     * @inheritDoc
     */
    public BlockEntityTypeRegistryAdapter(Registry<BlockEntityType<?>> registry, String modId) {
        super(registry, modId);
    }

    /**
     * @inheritDoc
     */
    public BlockEntityTypeRegistryAdapter(Registry<BlockEntityType<?>> registry) {
        super(registry);
    }

    /**
     * Gets the data fixer type for the tile entity instance
     *
     * @param name Tile entity name
     * @return Data fixer type
     */
    @Nullable
    private Type<?> getType(String name) {
        return Util.getChoiceType(TypeReferences.BLOCK_ENTITY, this.resourceName(name));
    }

    /**
     * Registers a tile entity type for a single block
     *
     * @param factory Tile entity factory
     * @param block   Single block to add
     * @param name    Tile entity name
     * @param <T>     Tile entity type
     * @return Registry object instance
     */
    @SuppressWarnings("ConstantConditions")
    public <T extends BlockEntity> BlockEntityType<T> register(BlockEntityFactory<? extends T> factory, Block block, String name) {
        return this.register(BlockEntityType.Builder.<T>create(factory, block).build(this.getType(name)), name);
    }

    /**
     * Registers a new tile entity type using a tile entity factory and an enum object
     *
     * @param name    Tile entity name
     * @param factory Tile entity factory
     * @param blocks  Enum object instance
     * @param <T>     Tile entity type
     * @return Tile entity type registry object
     */
    @SuppressWarnings("ConstantConditions")
    public <T extends BlockEntity> BlockEntityType<T> register(BlockEntityFactory<? extends T> factory, EnumObject<?, ? extends Block> blocks, String name) {
        return this.register(new BlockEntityType<>(factory, ImmutableSet.copyOf(blocks.values()), this.getType(name)), name);
    }

    /**
     * Registers a new tile entity type using a tile entity factory and an immutable set builder
     *
     * @param factory        Tile entity factory
     * @param name           Tile entity name
     * @param blockCollector Function to get blocks for the list
     * @param <T>            Tile entity type
     * @return Tile entity type registry object
     */
    @SuppressWarnings("ConstantConditions")
    public <T extends BlockEntity> BlockEntityType<T> register(BlockEntityFactory<? extends T> factory, String name, Consumer<Builder<Block>> blockCollector) {
        Builder<Block> blocks = new Builder<>();
        blockCollector.accept(blocks);
        return this.register(new BlockEntityType<>(factory, blocks.build(), this.getType(name)), name);
    }
}
