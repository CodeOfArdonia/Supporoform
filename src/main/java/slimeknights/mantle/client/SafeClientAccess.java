package slimeknights.mantle.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

/**
 * Class to add one level of static indirection to client only lookups
 */
public class SafeClientAccess {
    /**
     * Gets the currently pressed key for tooltips, returns UNKNOWN on a server
     */
    public static TooltipKey getTooltipKey() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientOnly.getPressedKey();
        }
        return TooltipKey.UNKNOWN;
    }

    /**
     * Gets the client player entity, or null on a server
     */
    @Nullable
    public static PlayerEntity getPlayer() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientOnly.getClientPlayer();
        }
        return null;
    }

    /**
     * Gets the client player entity, or null on a server
     */
    @Nullable
    public static World getLevel() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientOnly.getClientLevel();
        }
        return null;
    }

    /**
     * Gets the registry access client side
     */
    @Nullable
    public static DynamicRegistryManager getRegistryAccess() {
        World level = getLevel();
        if (level != null) {
            return level.getRegistryManager();
        }
        return null;
    }

    /**
     * This class is only loaded on the client, so is safe to reference client only methods
     */
    private static class ClientOnly {
        /**
         * Gets the currently pressed key modifier for tooltips
         */
        public static TooltipKey getPressedKey() {
            if (Screen.hasShiftDown()) {
                return TooltipKey.SHIFT;
            }
            if (Screen.hasControlDown()) {
                return TooltipKey.CONTROL;
            }
            if (Screen.hasAltDown()) {
                return TooltipKey.ALT;
            }
            return TooltipKey.NORMAL;
        }

        /**
         * Gets the client player instance
         */
        @Nullable
        public static PlayerEntity getClientPlayer() {
            return MinecraftClient.getInstance().player;
        }

        /**
         * Gets the client level instance
         */
        @Nullable
        public static World getClientLevel() {
            return MinecraftClient.getInstance().world;
        }
    }
}
