package slimeknights.mantle.registration.deferred;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.types.Type;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BlockEntityType.BlockEntityFactory;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.tconstruct.TConstruct;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Deferred register to register tile entity instances
 */
public class BlockEntityTypeDeferredRegister extends DeferredRegisterWrapper<BlockEntityType<?>> {
    public BlockEntityTypeDeferredRegister(String modID) {
        super(modID);
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
     * @param name    Tile entity name
     * @param factory Tile entity factory
     * @param block   Single block to add
     * @param <T>     Tile entity type
     * @return Registry object instance
     */
    @SuppressWarnings("ConstantConditions")
    public <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> register(String name, BlockEntityFactory<? extends T> factory, Supplier<? extends Block> block) {
        return RegistryEntry.of(Registry.register(Registries.BLOCK_ENTITY_TYPE, TConstruct.getResource(name), BlockEntityType.Builder.<T>create(factory, block.get()).build(this.getType(name))));
    }

    /**
     * Registers a new tile entity type using a tile entity factory and a block supplier
     *
     * @param name    Tile entity name
     * @param factory Tile entity factory
     * @param blocks  Enum object
     * @param <T>     Tile entity type
     * @return Tile entity type registry object
     */
    @SuppressWarnings("ConstantConditions")
    public <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> register(String name, BlockEntityFactory<? extends T> factory, EnumObject<?, ? extends Block> blocks) {
        return RegistryEntry.of(Registry.register(Registries.BLOCK_ENTITY_TYPE,TConstruct.getResource(name), new BlockEntityType<>(factory, ImmutableSet.copyOf(blocks.values()), this.getType(name))));
    }

    /**
     * Registers a new tile entity type using a tile entity factory and a block supplier
     *
     * @param name           Tile entity name
     * @param factory        Tile entity factory
     * @param blockCollector Function to get block list
     * @param <T>            Tile entity type
     * @return Tile entity type registry object
     */
    @SuppressWarnings("ConstantConditions")
    public <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> register(String name, BlockEntityFactory<? extends T> factory, Consumer<ImmutableSet.Builder<Block>> blockCollector) {
        Supplier<BlockEntityType<T>> blockEntityTypeSupplier = () -> {
            ImmutableSet.Builder<Block> blocks = new ImmutableSet.Builder<>();
            blockCollector.accept(blocks);
            return new BlockEntityType<>(factory, blocks.build(), this.getType(name));
        };
        return RegistryEntry.of(Registry.register(Registries.BLOCK_ENTITY_TYPE,
                TConstruct.getResource(name), blockEntityTypeSupplier.get()));
    }
}
