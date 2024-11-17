package slimeknights.tconstruct.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import slimeknights.mantle.inventory.BaseContainerMenu;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.world.TinkerWorld;

@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = TConstruct.MOD_ID)
public class CommonsEvents {

    // Slimy block jump stuff
    @SubscribeEvent
    static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        // check if we jumped from a slime block
        BlockPos pos = new BlockPos(event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ());
        if (event.getEntity().getCommandSenderWorld().isEmptyBlock(pos)) {
            pos = pos.down();
        }
        BlockState state = event.getEntity().getCommandSenderWorld().getBlockState(pos);
        Block block = state.getBlock();

        if (TinkerWorld.congealedSlime.contains(block)) {
            bounce(event.getEntity(), 0.25f);
        } else if (state.isIn(TinkerTags.Blocks.SLIMY_SOIL)) {
            bounce(event.getEntity(), 0.06f);
        }
    }

    /**
     * Handles opening our containers as the vanilla logic does not grant TE access
     */
    @SubscribeEvent
    static void openSpectatorMenu(RightClickBlock event) {
        PlayerEntity player = event.getEntity();
        if (player.isSpectator()) {
            BlockPos pos = event.getPos();
            World world = event.getLevel();
            BlockState state = world.getBlockState(pos);
            // only handle our blocks, no guarantee this will work with other mods
            if (TConstruct.MOD_ID.equals(Registry.BLOCK.getKey(state.getBlock()).getNamespace())) {
                NamedScreenHandlerFactory provider = state.createScreenHandlerFactory(world, pos);
                event.setCanceled(true);
                if (provider != null) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        NetworkHooks.openScreen(serverPlayer, provider, pos);
                        if (player.currentScreenHandler instanceof BaseContainerMenu<?> menu) {
                            menu.syncOnOpen(serverPlayer);
                        }
                    }
                    event.setCancellationResult(ActionResult.SUCCESS);
                }
                event.setCancellationResult(ActionResult.PASS);
            }
        }
    }

    private static void bounce(Entity entity, float amount) {
        entity.setVelocity(entity.getVelocity().add(0.0D, amount, 0.0D));
        entity.playSound(Sounds.SLIMY_BOUNCE.getSound(), 0.5f + amount, 1f);
    }
}
