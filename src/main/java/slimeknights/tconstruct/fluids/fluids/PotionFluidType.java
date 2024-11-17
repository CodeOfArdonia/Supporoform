package slimeknights.tconstruct.fluids.fluids;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import slimeknights.mantle.fluid.texture.ClientTextureFluidType;
import slimeknights.tconstruct.fluids.TinkerFluids;

import java.util.function.Consumer;

public class PotionFluidType extends FluidType {
    public PotionFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public String getDescriptionId(FluidStack stack) {
        return PotionUtil.getPotion(stack.getTag()).finishTranslationKey("item.minecraft.potion.effect.");
    }

    @Override
    public ItemStack getBucket(FluidStack fluidStack) {
        ItemStack itemStack = new ItemStack(fluidStack.getFluid().getBucketItem());
        itemStack.setNbt(fluidStack.getTag());
        return itemStack;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new ClientTextureFluidType(this) {
            /**
             * Gets the color, based on {@link PotionUtil#getColor(ItemStack)}
             * @param stack  Fluid stack instance
             * @return Color for the fluid
             */
            @Override
            public int getTintColor(FluidStack stack) {
                NbtCompound tag = stack.getTag();
                if (tag != null && tag.contains("CustomPotionColor", NbtElement.NUMBER_TYPE)) {
                    return tag.getInt("CustomPotionColor") | 0xFF000000;
                }
                if (PotionUtil.getPotion(tag) == Potions.EMPTY) {
                    return getTintColor();
                }
                return PotionUtil.getColor(PotionUtil.getPotionEffects(tag)) | 0xFF000000;
            }
        });
    }

    /**
     * Creates a fluid stack for the given potion
     */
    public static FluidStack potionFluid(Potion potion, int size) {
        NbtCompound tag = null;
        if (potion != Potions.EMPTY) {
            tag = new NbtCompound();
            tag.putString("Potion", Registries.POTION.getKey(potion).toString());
        }
        return new FluidStack(TinkerFluids.potion.get(), size, tag);
    }
}
