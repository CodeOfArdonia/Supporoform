package slimeknights.tconstruct.library.modifiers.fluid.block;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;

/**
 * Effect to place a block in using logic similar to block item placement.
 */
public record PlaceBlockFluidEffect(Block block) implements FluidEffect<FluidEffectContext.Block> {
    public static final RecordLoadable<PlaceBlockFluidEffect> LOADER = RecordLoadable.create(Loadables.BLOCK.requiredField("block", e -> e.block), PlaceBlockFluidEffect::new);

    @Override
    public RecordLoadable<PlaceBlockFluidEffect> getLoader() {
        return LOADER;
    }

    @Override
    public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext.Block context, FluidAction action) {
        if (level.isFull()) {
            // build the context
            ItemPlacementContext placeContext = new ItemPlacementContext(context.getLevel(), context.getPlayer(), Hand.MAIN_HAND, new ItemStack(this.block), context.getHitResult());
            if (placeContext.canPlace()) {
                // if we have a blockitem, we can offload a lot of the logic to it
                if (this.block.asItem() instanceof BlockItem blockItem) {
                    if (action.execute()) {
                        return blockItem.place(placeContext).isAccepted() ? 1 : 0;
                    }
                    // simulating is trickier but the methods exist
                    placeContext = blockItem.getPlacementContext(placeContext);
                    if (placeContext == null) {
                        return 0;
                    }
                }
                // following code is based on block item, with notably differences of not calling block item methods (as if we had one we'd use it above)
                // we do notably call this logic in simulation as we need to stop the block item logic early, differences are noted in comments with their vanilla impacts

                // simulate note: we don't ask the block item for its state for placement as that method is protected, this notably affects signs/banners (unlikely need)
                BlockState state = this.block.getPlacementState(placeContext);
                if (state == null) {
                    return 0;
                }
                // simulate note: we don't call BlockItem#canPlace as its protected, though never overridden in vanilla
                PlayerEntity player = context.getPlayer();
                World world = context.getLevel();
                BlockPos clicked = placeContext.getBlockPos();
                if (!state.canPlaceAt(world, clicked) || !world.canPlace(state, clicked, player == null ? ShapeContext.absent() : ShapeContext.of(player))) {
                    return 0;
                }
                // at this point the only check we are missing on simulate is actually placing the block failing
                if (action.execute()) {
                    // actually place the block
                    if (!world.setBlockState(clicked, state, Block.field_31022)) {
                        return 0;
                    }
                    // if its the expected block, run some criteria stuffs
                    BlockState placed = world.getBlockState(clicked);
                    if (placed.isOf(this.block)) {
                        // difference from BlockItem: do not update block state or block entity from tag as we have no tag
                        // it might however be worth passing in a set of properties to set here as part of JSON
                        // setPlacedBy probably won't be useful as w lack a tag, but who knw
                        ItemStack dummyStack = placeContext.getStack();
                        this.block.onPlaced(world, clicked, placed, player, dummyStack);
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            Criteria.PLACED_BLOCK.trigger(serverPlayer, clicked, dummyStack);
                        }
                    }

                    // resulting events
                    LivingEntity placer = context.getEntity(); // possible that living is nonnull when player is null
                    world.emitGameEvent(GameEvent.BLOCK_PLACE, clicked, GameEvent.Emitter.of(placer, placed));
                    BlockSoundGroup sound = placed.getSoundGroup();
                    world.playSound(null, clicked, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
                }
                return 1;
            }
        }
        return 0;
    }
}
