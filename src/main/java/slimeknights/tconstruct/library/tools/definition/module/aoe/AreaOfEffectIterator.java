package slimeknights.tconstruct.library.tools.definition.module.aoe;

import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Logic for iterating over a set of blocks
 */
public interface AreaOfEffectIterator {

    /**
     * Interface for loadable area of effect iterators, used for the fallback AOE iterator
     */
    interface Loadable extends AreaOfEffectIterator, ToolModule {
        List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<Loadable>defaultHooks(ToolHooks.AOE_ITERATOR);

        @Override
        default List<ModuleHook<?>> getDefaultHooks() {
            return DEFAULT_HOOKS;
        }
    }

    /**
     * Registry of all AOE loaders
     */
    GenericLoaderRegistry<Loadable> LOADER = new GenericLoaderRegistry<>("AOE Iterator", false);

    /**
     * Registers a loader with both tool modules and area of effect (latter used for fallback loader)
     */
    static void register(Identifier name, IGenericLoader<? extends Loadable> loader) {
        ToolModule.LOADER.register(name, loader);
        LOADER.register(name, loader);
    }

    /**
     * Gets a list of blocks that the tool can affect.
     *
     * @param tool      tool stack
     * @param stack     item stack for vanilla methods
     * @param world     the current world
     * @param player    the player using the tool
     * @param origin    the origin block spot to start from
     * @param sideHit   side of the block that was hit
     * @param matchType Type of match
     * @return A list of BlockPos's that the AOE tool can affect. Note these positions will likely be mutable
     */
    Iterable<BlockPos> getBlocks(IToolStackView tool, ItemStack stack, PlayerEntity player, BlockState state, World world, BlockPos origin, Direction sideHit, AOEMatchType matchType);

    /**
     * Gets the predicate for whether a given position can be broken in AOE
     *
     * @param tool      Tool used
     * @param stack     Item stack, for vanilla hooks
     * @param world     Level instance
     * @param origin    Center position
     * @param matchType Match logic
     * @return Predicate for AOE block matching
     */
    static Predicate<BlockPos> defaultBlockPredicate(IToolStackView tool, ItemStack stack, World world, BlockPos origin, AOEMatchType matchType) {
        // requires effectiveness
        if (matchType == AOEMatchType.BREAKING) {
            // don't let hardness vary too much
            float refHardness = world.getBlockState(origin).getHardness(world, origin);
            return pos -> {
                BlockState state = world.getBlockState(pos);
                if (state.isAir()) {
                    return false;
                }
                // if the hardness varies by too much, don't allow breaking
                float hardness = state.getHardness(world, pos);
                if (hardness == -1) {
                    return false;
                }
                if (refHardness == 0 ? hardness == 0 : hardness / refHardness <= 3) {
                    // must not be broken, and the tool definition must be effective
                    return IsEffectiveToolHook.isEffective(tool, state);
                }
                return false;
            };
        } else {
            return pos -> !world.isAir(pos);
        }
    }

    /**
     * Match types for the AOE getter
     */
    enum AOEMatchType {
        /**
         * Used when the block is being broken, typically matches only harvestable blocks
         * When using this type, the iteratable should be fetched before breaking the block
         */
        BREAKING,
        /**
         * Used for right click interactions such as hoeing, typically matches any block (will filter later)
         */
        TRANSFORM
    }
}
