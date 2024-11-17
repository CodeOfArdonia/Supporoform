package slimeknights.tconstruct.library.client;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.loading.FMLEnvironment;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.tconstruct.library.client.model.block.TankModel.Baked;

/**
 * This class contains various methods that are safe to call on both sides, which internally call client only code.
 */
public class SafeClient {
    /**
     * Triggers a model update if needed for this tank block
     *
     * @param be        Block entity instance
     * @param tank      Fluid tank instance
     * @param oldAmount Old fluid amount
     * @param newAmount New fluid amount
     */
    public static void updateFluidModel(BlockEntity be, FluidTank tank, int oldAmount, int newAmount) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientOnly.updateFluidModel(be, tank, oldAmount, newAmount);
        }
    }

    /**
     * This class is only ever loaded client side
     */
    private static class ClientOnly {
        /**
         * @see SafeClient#updateFluidModel(BlockEntity, FluidTank, int, int)
         */
        public static void updateFluidModel(BlockEntity be, FluidTank tank, int oldAmount, int newAmount) {
            World level = be.getWorld();
            if (level != null && level.isClient) {
                // if the amount change is bigger than a single increment, or we changed whether we have a fluid, update the world renderer
                BlockState state = be.getCachedState();
                Baked<?> model = ModelHelper.getBakedModel(state, Baked.class);
                if (model != null && (Math.abs(newAmount - oldAmount) >= (tank.getCapacity() / model.getFluid().getIncrements()) || (oldAmount == 0) != (newAmount == 0))) {
                    be.requestModelDataUpdate();
                    MinecraftClient.getInstance().worldRenderer.updateBlock(level, be.getPos(), state, state, 3);
                }
            }
        }
    }
}
