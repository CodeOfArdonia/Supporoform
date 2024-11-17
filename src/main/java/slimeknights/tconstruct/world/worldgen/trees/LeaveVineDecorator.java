package slimeknights.tconstruct.world.worldgen.trees;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.VineBlock;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.treedecorator.TreeDecorator;
import net.minecraft.world.gen.treedecorator.TreeDecoratorType;
import slimeknights.tconstruct.world.TinkerStructures;

/**
 * Recreation of {@link net.minecraft.world.gen.treedecorator.LeavesVineTreeDecorator} with variable vine type.
 */
@SuppressWarnings("deprecation")
@RequiredArgsConstructor
public class LeaveVineDecorator extends TreeDecorator {
    public static final Codec<LeaveVineDecorator> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Registry.BLOCK.byNameCodec().fieldOf("vines").forGetter(d -> d.vines),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(d -> d.probability)
            ).apply(inst, LeaveVineDecorator::new));

    private final Block vines;
    private final float probability;

    @Override
    protected TreeDecoratorType<?> getType() {
        return TinkerStructures.leaveVineDecorator.get();
    }

    @Override
    public void generate(TreeDecorator.Generator context) {
        Random random = context.getRandom();
        context.getLeavesPositions().forEach((pos) -> {
            for (Direction direction : Type.HORIZONTAL) {
                if (random.nextFloat() < this.probability) {
                    BlockPos offset = pos.offset(direction);
                    if (context.isAir(offset)) {
                        addHangingVine(offset, VineBlock.getFacingProperty(direction.getOpposite()), context);
                    }
                }
            }
        });
    }

    /**
     * Places vines at the given position
     */
    private void placeVine(BlockPos pos, BooleanProperty property, TreeDecorator.Generator context) {
        context.replace(pos, vines.getDefaultState().with(property, Boolean.TRUE));
    }

    /**
     * Adds a hanging vine around the given position
     */
    private void addHangingVine(BlockPos pos, BooleanProperty property, TreeDecorator.Generator context) {
        placeVine(pos, property, context);
        int i = 4;

        for (BlockPos target = pos.down(); context.isAir(target) && i > 0; --i) {
            placeVine(target, property, context);
            target = target.down();
        }
    }
}
