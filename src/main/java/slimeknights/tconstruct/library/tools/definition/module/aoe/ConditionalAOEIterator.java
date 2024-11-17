package slimeknights.tconstruct.library.tools.definition.module.aoe;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Iterator that tries one iterator, falling back to a second if the block does not match a tag
 *
 * @param condition Predicate to check the block against
 * @param ifTrue    Iterator to use if the block matches the tag
 * @param ifFalse   Iterator to use if the block does not match the tag
 */
public record ConditionalAOEIterator(IJsonPredicate<BlockState> condition, Loadable ifTrue,
                                     Loadable ifFalse) implements AreaOfEffectIterator.Loadable {
    public static final RecordLoadable<ConditionalAOEIterator> LOADER = RecordLoadable.create(
            BlockPredicate.LOADER.requiredField("condition", ConditionalAOEIterator::condition),
            AreaOfEffectIterator.LOADER.requiredField("if_true", ConditionalAOEIterator::ifTrue),
            AreaOfEffectIterator.LOADER.requiredField("if_false", ConditionalAOEIterator::ifFalse),
            ConditionalAOEIterator::new);

    @Override
    public RecordLoadable<ConditionalAOEIterator> getLoader() {
        return LOADER;
    }

    @Override
    public Iterable<BlockPos> getBlocks(IToolStackView tool, ItemStack stack, PlayerEntity player, BlockState state, World world, BlockPos origin, Direction sideHit, AOEMatchType matchType) {
        AreaOfEffectIterator iterator = this.condition.matches(state) ? this.ifTrue : this.ifFalse;
        return iterator.getBlocks(tool, stack, player, state, world, origin, sideHit, matchType);
    }
}
