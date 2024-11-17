package slimeknights.tconstruct.smeltery.block.controller;

import slimeknights.mantle.block.InventoryBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedBlock;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/**
 * Shared logic for all multiblock structure controllers
 */
public abstract class ControllerBlock extends InventoryBlock {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    public static final BooleanProperty IN_STRUCTURE = SearedBlock.IN_STRUCTURE;

    protected ControllerBlock(Settings builder) {
        super(builder);
        this.setDefaultState(this.getDefaultState().with(ACTIVE, false).with(IN_STRUCTURE, false));
    }


    /*
     * Block state
     */

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, IN_STRUCTURE);
    }

    @Nullable
    @Override
    public PathNodeType getBlockPathType(BlockState state, BlockView level, BlockPos pos, @Nullable MobEntity mob) {
        return state.get(IN_STRUCTURE) ? PathNodeType.DAMAGE_FIRE : PathNodeType.OPEN;
    }


    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Deprecated
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Deprecated
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(FACING, mirror.apply(state.get(FACING)));
    }


    /*
     * Tile Entity interaction
     */

    /**
     * @return True if the GUI can be opened
     */
    protected boolean canOpenGui(BlockState state) {
        return state.get(IN_STRUCTURE);
    }

    /**
     * Displays the multiblock's status, typically an error that it cannot form
     */
    protected boolean displayStatus(PlayerEntity player, World world, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    protected boolean openGui(PlayerEntity player, World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() == this) {
            if (canOpenGui(state)) {
                return super.openGui(player, world, pos);
            } else {
                return displayStatus(player, world, pos, state);
            }
        }
        return false;
    }


    /*
     * Particles
     */

    /**
     * Spawns fire particles at the given location
     *
     * @param world World instance
     * @param state Block state
     * @param x     Block X position
     * @param y     Block Y position
     * @param z     Block Z position
     * @param front Block front
     * @param side  Block side offset
     */
    protected void spawnFireParticles(WorldAccess world, BlockState state, double x, double y, double z, double front, double side) {
        spawnFireParticles(world, state, x, y, z, front, side, ParticleTypes.FLAME);
    }

    /**
     * Spawns fire particles at the given location
     *
     * @param world    World instance
     * @param state    Block state
     * @param x        Block X position
     * @param y        Block Y position
     * @param z        Block Z position
     * @param front    Block front
     * @param side     Block side offset
     * @param particle Particle to draw
     */
    protected void spawnFireParticles(WorldAccess world, BlockState state, double x, double y, double z, double front, double side, ParticleEffect particle) {
        switch (state.get(FACING)) {
            case WEST -> {
                world.addParticle(ParticleTypes.SMOKE, x - front, y, z + side, 0.0D, 0.0D, 0.0D);
                world.addParticle(particle, x - front, y, z + side, 0.0D, 0.0D, 0.0D);
            }
            case EAST -> {
                world.addParticle(ParticleTypes.SMOKE, x + front, y, z + side, 0.0D, 0.0D, 0.0D);
                world.addParticle(particle, x + front, y, z + side, 0.0D, 0.0D, 0.0D);
            }
            case NORTH -> {
                world.addParticle(ParticleTypes.SMOKE, x + side, y, z - front, 0.0D, 0.0D, 0.0D);
                world.addParticle(particle, x + side, y, z - front, 0.0D, 0.0D, 0.0D);
            }
            case SOUTH -> {
                world.addParticle(ParticleTypes.SMOKE, x + side, y, z + front, 0.0D, 0.0D, 0.0D);
                world.addParticle(particle, x + side, y, z + front, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
