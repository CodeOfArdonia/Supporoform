package slimeknights.tconstruct.plugin.jsonthings;

import dev.gigaherz.jsonthings.things.IFlexBlock;
import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import dev.gigaherz.jsonthings.things.serializers.IBlockSerializer;
import net.minecraft.block.Block;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.util.Lazy;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.plugin.jsonthings.block.FlexBurningLiquidBlock;
import slimeknights.tconstruct.plugin.jsonthings.block.FlexMobEffectLiquidBlock;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Collection of custom block types added by Tinkers
 */
public class FlexBlockTypes {
    /**
     * Creates the supplier for a fluid in a fluid block
     */
    private static Supplier<FlowableFluid> fluidSupplier(Identifier name) {
        return Lazy.of(() -> {
            // TODO: make Mantle loadables resource location
            if (((ResourceLocationLoadable<Fluid>) Loadables.FLUID).fromKey(name, "fluid") instanceof FlowingFluid flowing) {
                return flowing;
            } else {
                throw new RuntimeException("LiquidBlock requires a flowing fluid");
            }
        });
    }

    /**
     * Initializes the block types
     */
    public static void init() {
        register("burning_liquid", data -> {
            ResourceLocation fluidField = Loadables.RESOURCE_LOCATION.getOrDefault(data, "fluid", null);
            int burnTime = GsonHelper.getAsInt(data, "burn_time");
            float damage = GsonHelper.getAsFloat(data, "damage");
            return (props, builder) -> {
                final List<Property<?>> _properties = builder.getProperties();
                return new FlexBurningLiquidBlock(props, builder.getPropertyDefaultValues(), fluidSupplier(Objects.requireNonNullElse(fluidField, builder.getRegistryName())), burnTime, damage) {
                    @Override
                    protected void createBlockStateDefinition(Builder<Block, BlockState> stateBuilder) {
                        super.createBlockStateDefinition(stateBuilder);
                        _properties.forEach(stateBuilder::add);
                    }
                };
            };
        }, Material.LAVA);
        register("mob_effect_liquid", data -> {
            ResourceLocation fluidField = Loadables.RESOURCE_LOCATION.getOrDefault(data, "fluid", null);
            ResourceLocation effectName = Loadables.RESOURCE_LOCATION.getIfPresent(data, "effect");
            int effectLevel = GsonHelper.getAsInt(data, "burn_time");
            return (props, builder) -> {
                final List<Property<?>> _properties = builder.getProperties();
                Lazy<MobEffect> effect = Lazy.of(() -> ((ResourceLocationLoadable<MobEffect>) Loadables.MOB_EFFECT).fromKey(effectName, "effect"));
                return new FlexMobEffectLiquidBlock(props, builder.getPropertyDefaultValues(), fluidSupplier(Objects.requireNonNullElse(fluidField, builder.getRegistryName())), () -> new MobEffectInstance(effect.get(), 5 * 20, effectLevel - 1)) {
                    @Override
                    protected void createBlockStateDefinition(Builder<Block, BlockState> stateBuilder) {
                        super.createBlockStateDefinition(stateBuilder);
                        _properties.forEach(stateBuilder::add);
                    }
                };
            };
        }, Material.WATER);
    }

    /**
     * Local helper to register our stuff
     */
    private static <T extends Block & IFlexBlock> void register(String name, IBlockSerializer<T> factory, Material defaultMaterial) {
        FlexBlockType.register(TConstruct.resourceString(name), factory, "translucent", true, defaultMaterial);
    }
}