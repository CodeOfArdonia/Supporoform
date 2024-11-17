package slimeknights.tconstruct.world.worldgen.trees;

import com.mojang.serialization.Codec;
import slimeknights.tconstruct.world.TinkerStructures;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.stateprovider.BlockStateProviderType;

public class SupplierBlockStateProvider extends BlockStateProvider {

    public static final Codec<SupplierBlockStateProvider> TYPE_CODEC =
            BlockState.CODEC.fieldOf("state")
                    .xmap(SupplierBlockStateProvider::new, (provider) -> provider.supplier.get()).codec();

    private final Supplier<BlockState> supplier;

    private SupplierBlockStateProvider(BlockState state) {
        this(() -> state);
    }

    public SupplierBlockStateProvider(Supplier<BlockState> blockStateSupplier) {
        this.supplier = blockStateSupplier;
    }

    /**
     * Create a supplier for a state
     */
    public static SupplierBlockStateProvider ofState(Supplier<BlockState> supplier) {
        return new SupplierBlockStateProvider(supplier);
    }

    /**
     * Create a supplier for a blocks default state
     */
    public static SupplierBlockStateProvider ofBlock(Supplier<? extends Block> supplier) {
        return ofState(() -> supplier.get().getDefaultState());
    }

    @Override
    protected BlockStateProviderType<?> getType() {
        return TinkerStructures.supplierBlockstateProvider.get();
    }

    @Override
    public BlockState get(Random randomIn, BlockPos blockPosIn) {
        return this.supplier.get();
    }
}
