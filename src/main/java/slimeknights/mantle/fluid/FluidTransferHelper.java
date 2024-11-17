package slimeknights.mantle.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.fluids.sound.SoundAction;
import io.github.fabricators_of_create.porting_lib.fluids.sound.SoundActions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;

/**
 * Alternative to {@link net.minecraftforge.fluids.FluidUtil} since no one has time to make the forge util not a buggy mess
 */
@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FluidTransferHelper {
    private static final String KEY_FILLED = Mantle.makeDescriptionId("block", "tank.filled");
    private static final String KEY_DRAINED = Mantle.makeDescriptionId("block", "tank.drained");

    /**
     * Gets the given sound from the fluid
     */
    public static SoundEvent getSound(FluidStack fluid, SoundAction action, SoundEvent fallback) {
        SoundEvent event = fluid.getFluid().getFluidType().getSound(fluid, action);
        if (event == null) {
            return fallback;
        }
        return event;
    }

    /**
     * Gets the empty sound for a fluid
     */
    public static SoundEvent getEmptySound(FluidStack fluid) {
        return getSound(fluid, SoundActions.BUCKET_EMPTY, SoundEvents.ITEM_BUCKET_EMPTY);
    }

    /**
     * Gets the fill sound for a fluid
     */
    public static SoundEvent getFillSound(FluidStack fluid) {
        return getSound(fluid, SoundActions.BUCKET_FILL, SoundEvents.ITEM_BUCKET_FILL);
    }

    /**
     * Attempts to transfer fluid
     *
     * @param input   Fluid source
     * @param output  Fluid destination
     * @param maxFill Maximum to transfer
     * @return True if transfer succeeded
     */
    public static FluidStack tryTransfer(IFluidHandler input, IFluidHandler output, int maxFill) {
        // first, figure out how much we can drain
        FluidStack simulated = input.drain(maxFill, FluidAction.SIMULATE);
        if (!simulated.isEmpty()) {
            // next, find out how much we can fill
            int simulatedFill = output.fill(simulated, FluidAction.SIMULATE);
            if (simulatedFill > 0) {
                // actually drain
                FluidStack drainedFluid = input.drain(simulatedFill, FluidAction.EXECUTE);
                if (!drainedFluid.isEmpty()) {
                    // acutally fill
                    int actualFill = output.fill(drainedFluid.copy(), FluidAction.EXECUTE);
                    if (actualFill != drainedFluid.getAmount()) {
                        Mantle.logger.error("Lost {} fluid during transfer", drainedFluid.getAmount() - actualFill);
                    }
                }
                return drainedFluid;
            }
        }
        return FluidStack.EMPTY;
    }

    /**
     * Attempts to interact with a flilled bucket on a fluid tank. This is unique as it handles fish buckets, which don't expose fluid capabilities
     *
     * @param world  World instance
     * @param pos    Block position
     * @param player Player
     * @param hand   Hand
     * @param hit    Hit side
     * @param offset Direction to place fish
     * @return True if using a bucket
     */
    public static boolean interactWithBucket(World world, BlockPos pos, PlayerEntity player, Hand hand, Direction hit, Direction offset) {
        ItemStack held = player.getStackInHand(hand);
        if (held.getItem() instanceof BucketItem bucket) {
            Fluid fluid = bucket.getFluid();
            if (fluid != Fluids.EMPTY) {
                if (!world.isClient) {
                    BlockEntity te = world.getBlockEntity(pos);
                    if (te != null) {
                        te.getCapability(ForgeCapabilities.FLUID_HANDLER, hit)
                                .ifPresent(handler -> {
                                    FluidStack fluidStack = new FluidStack(bucket.getFluid(), FluidType.BUCKET_VOLUME);
                                    // must empty the whole bucket
                                    if (handler.fill(fluidStack, FluidAction.SIMULATE) == FluidType.BUCKET_VOLUME) {
                                        SoundEvent sound = getEmptySound(fluidStack);
                                        handler.fill(fluidStack, FluidAction.EXECUTE);
                                        bucket.checkExtraContent(player, world, held, pos.relative(offset));
                                        world.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
                                        player.displayClientMessage(Component.translatable(KEY_FILLED, FluidType.BUCKET_VOLUME, fluidStack.getDisplayName()), true);
                                        if (!player.isCreative()) {
                                            player.setItemInHand(hand, held.getCraftingRemainingItem());
                                        }
                                    }
                                });
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Plays the sound from filling a TE
     */
    public static void playEmptySound(World world, BlockPos pos, PlayerEntity player, FluidStack transferred) {
        world.playSound(null, pos, getEmptySound(transferred), SoundCategory.BLOCKS, 1.0F, 1.0F);
        player.sendMessage(Text.translatable(KEY_FILLED, transferred.getAmount(), transferred.getDisplayName()), true);
    }

    /**
     * Plays the sound from draining a TE
     */
    public static void playFillSound(World world, BlockPos pos, PlayerEntity player, FluidStack transferred) {
        world.playSound(null, pos, getFillSound(transferred), SoundCategory.BLOCKS, 1.0F, 1.0F);
        player.sendMessage(Text.translatable(KEY_DRAINED, transferred.getAmount(), transferred.getDisplayName()), true);
    }

    /**
     * Base logic to interact with a tank
     *
     * @param world  World instance
     * @param pos    Tank position
     * @param player Player instance
     * @param hand   Hand used
     * @param hit    Hit position
     * @return True if further interactions should be blocked, false otherwise
     */
    public static boolean interactWithFluidItem(World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // success if the item is a fluid handler, regardless of if fluid moved
        ItemStack stack = player.getStackInHand(hand);
        Direction face = hit.getSide();
        // fetch capability before copying, bit more work when its a fluid handler, but saves copying time when its not
        if (!stack.isEmpty()) {
            // only server needs to transfer stuff
            BlockEntity te = world.getBlockEntity(pos);
            if (te != null) {
                // TE must have a capability
                LazyOptional<IFluidHandler> teCapability = te.getCapability(ForgeCapabilities.FLUID_HANDLER, face);
                if (teCapability.isPresent()) {
                    IFluidHandler teHandler = teCapability.orElse(EmptyFluidHandler.INSTANCE);

                    // fallback to JSON based transfer
                    if (FluidContainerTransferManager.INSTANCE.mayHaveTransfer(stack)) {
                        // only actually transfer on the serverside, client just has items
                        if (!world.isClient) {
                            FluidStack currentFluid = teHandler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
                            IFluidContainerTransfer transfer = FluidContainerTransferManager.INSTANCE.getTransfer(stack, currentFluid);
                            if (transfer != null) {
                                TransferResult result = transfer.transfer(stack, currentFluid, teHandler);
                                if (result != null) {
                                    if (result.didFill()) {
                                        playFillSound(world, pos, player, result.fluid());
                                    } else {
                                        playEmptySound(world, pos, player, result.fluid());
                                    }
                                    player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, result.stack()));
                                }
                            }
                        }
                        return true;
                    }

                    // if the item has a capability, do a direct transfer
                    ItemStack copy = ItemHandlerHelper.copyStackWithSize(stack, 1);
                    LazyOptional<IFluidHandlerItem> itemCapability = copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
                    if (itemCapability.isPresent()) {
                        if (!world.isClient) {
                            IFluidHandlerItem itemHandler = itemCapability.resolve().orElseThrow();
                            // first, try filling the TE from the item
                            FluidStack transferred = tryTransfer(itemHandler, teHandler, Integer.MAX_VALUE);
                            if (!transferred.isEmpty()) {
                                playEmptySound(world, pos, player, transferred);
                            } else {
                                // if that failed, try filling the item handler from the TE
                                transferred = tryTransfer(teHandler, itemHandler, Integer.MAX_VALUE);
                                if (!transferred.isEmpty()) {
                                    playFillSound(world, pos, player, transferred);
                                }
                            }
                            // if either worked, update the player's inventory
                            if (!transferred.isEmpty()) {
                                player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, itemHandler.getContainer()));
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Utility to try fluid item then bucket
     *
     * @param world  World instance
     * @param pos    Tank position
     * @param player Player instance
     * @param hand   Hand used
     * @param hit    Hit position
     * @return True if interacted
     */
    public static boolean interactWithTank(World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return interactWithFluidItem(world, pos, player, hand, hit)
                || interactWithBucket(world, pos, player, hand, hit.getSide(), hit.getSide());
    }
}
