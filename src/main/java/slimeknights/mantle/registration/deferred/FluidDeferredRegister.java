package slimeknights.mantle.registration.deferred;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.ForgeFlowingFluid.Properties;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.mantle.fluid.UnplaceableFluid;
import slimeknights.mantle.registration.DelayedSupplier;
import slimeknights.mantle.registration.FluidBuilder;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.FluidObject;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Deferred register solving the nightmare that is registering fluids with Forge
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FluidDeferredRegister extends DeferredRegisterWrapper<Fluid> {
    private final SynchronizedDeferredRegister<FluidType> fluidTypeRegister;
    private final SynchronizedDeferredRegister<Block> blockRegister;
    private final SynchronizedDeferredRegister<Item> itemRegister;

    public FluidDeferredRegister(String modID) {
        super(RegistryKeys.FLUID, modID);
        this.fluidTypeRegister = SynchronizedDeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, modID);
        this.blockRegister = SynchronizedDeferredRegister.create(RegistryKeys.BLOCK, modID);
        this.itemRegister = SynchronizedDeferredRegister.create(RegistryKeys.ITEM, modID);
    }

    @Override
    public void register(IEventBus bus) {
        super.register(bus);
        this.fluidTypeRegister.register(bus);
        this.blockRegister.register(bus);
        this.itemRegister.register(bus);
    }

    /**
     * Registers a fluid type to the registry
     *
     * @param name Name of the fluid to register
     * @param sup  Fluid supplier
     * @param <I>  Fluid type
     * @return Fluid to supply
     */
    public <I extends FluidType> RegistryObject<I> registerType(String name, Supplier<? extends I> sup) {
        return this.fluidTypeRegister.register(name, sup);
    }

    /**
     * Registers a fluid to the registry
     *
     * @param name Name of the fluid to register
     * @param sup  Fluid supplier
     * @param <I>  Fluid type
     * @return Fluid to supply
     */
    public <I extends Fluid> RegistryObject<I> registerFluid(String name, Supplier<? extends I> sup) {
        return this.register.register(name, sup);
    }

    /**
     * Starts a builder for a fluid
     */
    public Builder register(String name) {
        return new Builder(name);
    }

    @Accessors(fluent = true)
    @Setter
    public class Builder extends FluidBuilder<Builder> {
        private final String name;
        private final DelayedSupplier<Fluid> stillDelayed = new DelayedSupplier<>();
        /**
         * Name to use for the tag, defaults to the fluid name
         */
        private String tagName;

        private Builder(String name) {
            this.name = name;
            this.tagName = name;
        }

        /* Fluid type */

        /**
         * Registers the passed fluid type
         */
        public Builder type(Supplier<? extends FluidType> type) {
            if (this.type != null) {
                throw new IllegalStateException("Type already created for " + this.name);
            }
            this.type = FluidDeferredRegister.this.fluidTypeRegister.register(this.name, type);
            return this;
        }

        /**
         * Registers a fluid with the given properties, using the texture fluid type
         */
        public Builder type(FluidType.Properties properties) {
            return this.type(() -> new TextureFluidType(properties));
        }

        /**
         * Registers a fluid with the given properties, using the texture fluid type
         */
        public Builder type() {
            return type(FluidType.Properties.create());
        }


        /* Bucket */

        /**
         * Creates the bucket using the given supplier
         */
        public Builder bucket(Function<Supplier<? extends Fluid>, Item> constructor) {
            if (this.bucket != null) {
                throw new IllegalStateException("Bucket already created for " + this.name);
            }
            return bucket(FluidDeferredRegister.this.itemRegister.register(this.name + "_bucket", () -> constructor.apply(this.stillDelayed)));
        }

        /**
         * Creates the default bucket
         */
        public Builder bucket() {
            return bucket(FluidDeferredRegister.this.itemRegister.register(this.name + "_bucket", () -> new BucketItem(this.stillDelayed, RegistrationHelper.BUCKET_PROPS)));
        }


        /* Block */

        /**
         * Creates the block form using the given supplier
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        // if you are calling this method, you must have a flowing fluid by the end, we throw later if not
        public Builder block(Function<Supplier<? extends FlowableFluid>, FluidBlock> constructor) {
            if (this.block != null) {
                throw new IllegalStateException("Block already created for " + this.name);
            }
            return block(FluidDeferredRegister.this.blockRegister.register(this.name + "_fluid", () -> constructor.apply((Supplier<FlowableFluid>) (Supplier) this.stillDelayed)));
        }

        /**
         * Creates the default block from the given material and light level
         */
        public Builder block(MapColor color, int lightLevel) {
            return this.block(sup -> new FluidBlock(sup, AbstractBlock.Settings.create().mapColor(color).replaceable().noCollision().ticksRandomly().strength(100.0F).luminance(state -> lightLevel).pistonBehavior(PistonBehavior.DESTROY).dropsNothing().liquid().sounds(BlockSoundGroup.INTENTIONALLY_EMPTY)));
        }


        /* Final fluid */

        /**
         * Builds an unplacable fluid with the default constructor
         */
        public FluidObject<UnplaceableFluid> unplacable() {
            return this.unplacable(UnplaceableFluid::new);
        }

        /**
         * Builds an unplacable fluid with the passed constructor
         *
         * @param constructor Constructor taking a fluid type and bucket supplier. Note the bucket supplier may be null.
         * @param <F>         Resulting fluid type
         * @return Fluid object instance
         */
        public <F extends Fluid> FluidObject<F> unplacable(Function<FluidBuilder<?>, F> constructor) {
            if (this.block != null) {
                throw new IllegalStateException("Cannot build an unplacable fluid with a block form");
            }
            if (this.type == null) {
                this.type();
            }
            RegistryObject<F> fluid = FluidDeferredRegister.this.registerFluid(this.name, () -> constructor.apply(this));
            this.stillDelayed.setSupplier(fluid);
            return new FluidObject<>(FluidDeferredRegister.this.resource(this.name), this.tagName, this.type, fluid);
        }

        /**
         * Builds a flowing fluid with the default constructors
         */
        public FlowingFluidObject<ForgeFlowingFluid> flowing() {
            return this.flowing(ForgeFlowingFluid.Source::new, ForgeFlowingFluid.Flowing::new);
        }

        /**
         * Builds a flowing fluid with the given constructors
         *
         * @param createStill   Still constructor taking forge fluid properties, will contain the type, bucket, block, and flowing forms
         * @param createFlowing Flowing constructor taking forge fluid properties, will contain the type, bucket, block, and still forms
         * @param <F>           Type of fluids being created
         * @return Flowing fluid object instance
         */
        public <F extends FlowableFluid> FlowingFluidObject<F> flowing(Function<Properties, ? extends F> createStill, Function<Properties, ? extends F> createFlowing) {
            if (this.type == null) {
                this.type();
            }

            // create props with the suppliers
            DelayedSupplier<FlowableFluid> flowingDelayed = new DelayedSupplier<>();
            Properties props = this.build(this.type, this.stillDelayed, flowingDelayed);

            // create fluids now that we have props
            Supplier<F> still = FluidDeferredRegister.this.registerFluid(this.name, () -> createStill.apply(props));
            this.stillDelayed.setSupplier(still);
            Supplier<F> flowing = FluidDeferredRegister.this.registerFluid("flowing_" + this.name, () -> createFlowing.apply(props));
            flowingDelayed.setSupplier(flowing);

            // return the final nice object
            return new FlowingFluidObject<>(FluidDeferredRegister.this.resource(this.name), this.tagName, this.type, still, flowing, this.block);
        }
    }
}
