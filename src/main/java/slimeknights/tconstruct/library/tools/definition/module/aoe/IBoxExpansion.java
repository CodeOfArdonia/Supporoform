package slimeknights.tconstruct.library.tools.definition.module.aoe;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import slimeknights.mantle.data.registry.NamedComponentRegistry;
import slimeknights.tconstruct.TConstruct;

/**
 * Logic that determines how box AOE expands
 */
public interface IBoxExpansion {
    /**
     * Registered box expansion types
     */
    NamedComponentRegistry<IBoxExpansion> REGISTRY = new NamedComponentRegistry<>("Unknown Box Expansion Type");

    /**
     * Gets the directions to expand for the given player and side hit
     *
     * @param player  Player instance
     * @param sideHit Side of the block hit
     * @return Directions of expansion
     */
    ExpansionDirections getDirections(PlayerEntity player, Direction sideHit);

    /**
     * Computed direction of expansion
     */
    record ExpansionDirections(Direction width, Direction height, Direction depth, boolean traverseDown) {
    }

    /**
     * Expands a box around the targeted face
     */
    IBoxExpansion SIDE_HIT = REGISTRY.register(TConstruct.getResource("side_hit"), (player, sideHit) -> {
        // depth is always direction into the block
        Direction depth = sideHit.getOpposite();
        Direction width, height;
        // for Y, direction is based on facing
        if (sideHit.getAxis() == Axis.Y) {
            height = player.getHorizontalFacing();
            width = height.rotateYClockwise();
        } else {
            // for X and Z, just rotate from side hit
            width = sideHit.rotateYCounterclockwise();
            height = Direction.UP;
        }
        return new ExpansionDirections(width, height, depth, true);
    });

    /**
     * Box expansion based on the direction the player looks
     */
    IBoxExpansion PITCH = REGISTRY.register(TConstruct.getResource("pitch"), (player, sideHit) -> {
        // depth is always direction into the block
        Direction playerLook = player.getHorizontalFacing();
        float pitch = player.getPitch();
        Direction width = playerLook.rotateYClockwise();
        Direction depth, height;
        if (pitch < -60) {
            depth = Direction.UP;
            height = playerLook;
        } else if (pitch > 60) {
            depth = Direction.DOWN;
            height = playerLook;
        } else {
            height = Direction.UP;
            depth = playerLook;
        }
        return new ExpansionDirections(width, height, depth, true);
    });

    /**
     * Box expansion going up and additionally to a facing side
     */
    IBoxExpansion HEIGHT = REGISTRY.register(TConstruct.getResource("height"), (player, sideHit) -> {
        // if hit the top or bottom, use facing direction
        Direction depth;
        if (sideHit.getAxis().isVertical()) {
            depth = player.getHorizontalFacing();
        } else {
            depth = sideHit.getOpposite();
        }
        return new ExpansionDirections(depth.rotateYClockwise(), Direction.UP, depth, false);
    });
}
