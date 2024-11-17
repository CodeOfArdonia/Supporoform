package slimeknights.tconstruct.tools.modifiers.ability.tool;

import io.github.fabricators_of_create.porting_lib.fluids.sound.SoundActions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.behavior.ShowOffhandModule;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Objects;

import static slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper.TANK_HELPER;

public class BucketingModifier extends Modifier implements BlockInteractionModifierHook, GeneralInteractionModifierHook, EquipmentChangeModifierHook {
    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
        hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).flat(FluidType.BUCKET_VOLUME));
        hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
        hookBuilder.addHook(this, ModifierHooks.BLOCK_INTERACT, ModifierHooks.GENERAL_INTERACT);
        hookBuilder.addModule(ShowOffhandModule.ALLOW_BROKEN);
    }

    @Override
    public Text getDisplayName(IToolStackView tool, ModifierEntry entry) {
        return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, entry));
    }

    /**
     * Checks if the block is unable to contain fluid
     *
     * @param world Level
     * @param pos   Position to try
     * @param state State
     * @param fluid Fluid to place
     * @return True if the block is unable to contain fluid, false if it can contain fluid
     */
    private static boolean cannotContainFluid(World world, BlockPos pos, BlockState state, Fluid fluid) {
        Block block = state.getBlock();
        return !state.canBucketPlace(fluid) && !(block instanceof FluidFillable container && container.canFillWithFluid(world, pos, state, fluid));
    }

    @Override
    public ActionResult beforeBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        if (source != InteractionSource.ARMOR) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        BlockPos target = context.getBlockPos();
        // must have a TE that has a fluid handler capability
        BlockEntity te = world.getBlockEntity(target);
        if (te == null) {
            return ActionResult.PASS;
        }
        Direction face = context.getSide();
        LazyOptional<IFluidHandler> capability = te.getCapability(ForgeCapabilities.FLUID_HANDLER, face);
        if (!capability.isPresent()) {
            return ActionResult.PASS;
        }

        // only the server needs to deal with actually handling stuff
        if (!world.isClient) {
            PlayerEntity player = context.getPlayer();
            boolean sneaking = player != null && player.isSneaking();
            capability.ifPresent(cap -> {
                FluidStack fluidStack = TANK_HELPER.getFluid(tool);
                // sneaking fills, not sneak drains
                SoundEvent sound = null;
                if (sneaking) {
                    // must have something to fill
                    if (!fluidStack.isEmpty()) {
                        int added = cap.fill(fluidStack, FluidAction.EXECUTE);
                        if (added > 0) {
                            sound = FluidTransferHelper.getEmptySound(fluidStack);
                            fluidStack.shrink(added);
                            TANK_HELPER.setFluid(tool, fluidStack);
                        }
                    }
                    // if nothing currently, will drain whatever
                } else if (fluidStack.isEmpty()) {
                    FluidStack drained = cap.drain(TANK_HELPER.getCapacity(tool), FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        TANK_HELPER.setFluid(tool, drained);
                        sound = FluidTransferHelper.getFillSound(fluidStack);
                    }
                } else {
                    // filter drained to be the same as the current fluid
                    FluidStack drained = cap.drain(new FluidStack(fluidStack, TANK_HELPER.getCapacity(tool) - fluidStack.getAmount()), FluidAction.EXECUTE);
                    if (!drained.isEmpty() && drained.isFluidEqual(fluidStack)) {
                        fluidStack.grow(drained.getAmount());
                        TANK_HELPER.setFluid(tool, fluidStack);
                        sound = FluidTransferHelper.getFillSound(fluidStack);
                    }
                }
                if (sound != null) {
                    world.playSound(null, target, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
            });
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public ActionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        if (!tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            return ActionResult.PASS;
        }
        // only place fluid if sneaking, we contain at least a bucket, and its a block
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isSneaking()) {
            return ActionResult.PASS;
        }
        FluidStack fluidStack = TANK_HELPER.getFluid(tool);
        if (fluidStack.getAmount() < FluidType.BUCKET_VOLUME) {
            return ActionResult.PASS;
        }
        Fluid fluid = fluidStack.getFluid();
        if (!(fluid instanceof FlowableFluid)) {
            return ActionResult.PASS;
        }

        // can we interact with the position
        Direction face = context.getSide();
        World world = context.getWorld();
        BlockPos target = context.getBlockPos();
        BlockPos offset = target.offset(face);
        if (!world.canPlayerModifyAt(player, target) || !player.canPlaceOn(offset, face, context.getStack())) {
            return ActionResult.PASS;
        }

        // if the block cannot be placed at the current location, try placing at the neighbor
        BlockState existing = world.getBlockState(target);
        if (cannotContainFluid(world, target, existing, fluidStack.getFluid())) {
            target = offset;
            existing = world.getBlockState(target);
            if (cannotContainFluid(world, target, existing, fluidStack.getFluid())) {
                return ActionResult.PASS;
            }
        }

        // if water, evaporate
        boolean placed = false;
        if (world.getDimension().ultrawarm() && fluid.isIn(FluidTags.WATER)) {
            world.playSound(player, target, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
            for (int l = 0; l < 8; ++l) {
                world.addParticle(ParticleTypes.LARGE_SMOKE, target.getX() + Math.random(), target.getY() + Math.random(), target.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
            }
            placed = true;
        } else if (existing.canBucketPlace(fluid)) {
            // if its a liquid container, we should have validated it already
            if (!world.isClient && !existing.isLiquid()) {
                world.breakBlock(target, true);
            }
            if (world.setBlockState(target, fluid.getDefaultState().getBlockState()) || existing.getFluidState().isStill()) {
                world.playSound(null, target, FluidTransferHelper.getEmptySound(fluidStack), SoundCategory.BLOCKS, 1.0F, 1.0F);
                placed = true;
            }
        } else if (existing.getBlock() instanceof FluidFillable container) {
            // if not replaceable, it must be a liquid container
            container.tryFillWithFluid(world, target, existing, ((FlowableFluid) fluid).getStill(false));
            world.playSound(null, target, FluidTransferHelper.getEmptySound(fluidStack), SoundCategory.BLOCKS, 1.0F, 1.0F);
            placed = true;
        }

        // if we placed something, consume fluid
        if (placed) {
            fluidStack.shrink(FluidType.BUCKET_VOLUME);
            TANK_HELPER.setFluid(tool, fluidStack);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (player.isInSneakingPose() || !tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            return ActionResult.PASS;
        }

        // need at least a bucket worth of empty space
        FluidStack fluidStack = TANK_HELPER.getFluid(tool);
        if (TANK_HELPER.getCapacity(tool) - fluidStack.getAmount() < FluidType.BUCKET_VOLUME) {
            return ActionResult.PASS;
        }
        // have to trace again to find the fluid, ensure we can edit the position
        World world = player.getWorld();
        BlockHitResult trace = ModifiableItem.blockRayTrace(world, player, RaycastContext.FluidHandling.SOURCE_ONLY);
        if (trace.getType() != Type.BLOCK) {
            return ActionResult.PASS;
        }
        Direction face = trace.getSide();
        BlockPos target = trace.getBlockPos();
        BlockPos offset = target.offset(face);
        if (!world.canPlayerModifyAt(player, target) || !player.canPlaceOn(offset, face, player.getEquippedStack(source.getSlot(hand)))) {
            return ActionResult.PASS;
        }
        // try to find a fluid here
        FluidState fluidState = world.getFluidState(target);
        Fluid currentFluid = fluidStack.getFluid();
        if (fluidState.isEmpty() || (!fluidStack.isEmpty() && !currentFluid.matchesType(fluidState.getFluid()))) {
            return ActionResult.PASS;
        }
        // finally, pickup the fluid
        BlockState state = world.getBlockState(target);
        if (state.getBlock() instanceof FluidDrainable bucketPickup) {
            // TODO: not sure how to deal with this API change, this current method means we delete snow
            //Fluid pickedUpFluid = bucketPickup.takeLiquid(world, target, state);
            ItemStack bucket = bucketPickup.tryDrainFluid(world, target, state);
            if (!bucket.isEmpty() && bucket.getItem() instanceof BucketItem bucketItem) {
                Fluid pickedUpFluid = bucketItem.getFluid();
                if (pickedUpFluid != Fluids.EMPTY) {
                    player.playSound(Objects.requireNonNullElse(pickedUpFluid.getFluidType().getSound(SoundActions.BUCKET_FILL), SoundEvents.ITEM_BUCKET_FILL), 1.0F, 1.0F);
                    // set the fluid if empty, increase the fluid if filled
                    if (!world.isClient) {
                        if (fluidStack.isEmpty()) {
                            TANK_HELPER.setFluid(tool, new FluidStack(pickedUpFluid, FluidType.BUCKET_VOLUME));
                        } else if (pickedUpFluid == currentFluid) {
                            fluidStack.grow(FluidType.BUCKET_VOLUME);
                            TANK_HELPER.setFluid(tool, fluidStack);
                        } else {
                            TConstruct.LOG.error("Picked up a fluid {} that does not match the current fluid state {}, this should not happen", pickedUpFluid, fluidState.getFluid());
                        }
                    }
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }
}
