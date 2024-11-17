package slimeknights.tconstruct.library.client.modifiers;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.models.QuadTransformers;
import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.fluid.Fluid;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.SimpleModelState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.model.FluidContainerModel;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Model for tank modifiers, also displays the fluid
 */
public class FluidModifierModel extends NormalModifierModel {
    /**
     * Location used for baking dynamic models, name does not matter so just using a constant
     */
    private static final Identifier BAKE_LOCATION = TConstruct.getResource("dynamic_fluid_model");

    /**
     * The vanilla model bakery uses an orgin of 0.5,0.5,0.5, and forges dynamic fluid code uses the vanilla model bakery. (see{@link net.minecraft.client.render.model.BakedQuadFactory} {@code #rotateVertexBy()} for vanilla bakery)
     * However, item layer wants an origin of 0,0,0, which is what we expect in our tool models. So cancel out the origin.
     */
    private static final Vector3f ORIGIN = new Vector3f(-0.5f, -0.5f, -0.5f);

    /**
     * Constant unbaked model instance, as they are all the same
     */
    public static final IUnbakedModifierModel UNBAKED_INSTANCE = new Unbaked(ToolTankHelper.TANK_HELPER);

    /**
     * Logic for fetching the fluid
     */
    protected final ToolTankHelper helper;
    /**
     * Textures to show
     */
    protected final SpriteIdentifier[] fluidTextures;

    protected FluidModifierModel(ToolTankHelper helper, @Nullable SpriteIdentifier smallTexture, @Nullable SpriteIdentifier largeTexture, SpriteIdentifier[] fluidTextures) {
        super(smallTexture, largeTexture);
        this.helper = helper;
        this.fluidTextures = fluidTextures;
    }

    public FluidModifierModel(ToolTankHelper helper, @Nullable SpriteIdentifier smallTexture, @Nullable SpriteIdentifier largeTexture,
                              @Nullable SpriteIdentifier smallFull, @Nullable SpriteIdentifier largeFull) {
        this(helper, smallTexture, largeTexture, new SpriteIdentifier[]{smallFull, largeFull});
    }

    @Nullable
    @Override
    public Object getCacheKey(IToolStackView tool, ModifierEntry entry) {
        FluidStack fluid = this.helper.getFluid(tool);
        if (!fluid.isEmpty()) {
            // cache by modifier and fluid
            return new FluidModifierCacheKey(entry.getModifier(), fluid.getFluid());
        }
        return entry != ModifierEntry.EMPTY ? entry.getId() : null;
    }

    @Nullable
    protected SpriteIdentifier getTemplate(IToolStackView tool, ModifierEntry entry, FluidStack fluid, boolean isLarge) {
        return this.fluidTextures[(isLarge ? 1 : 0)];
    }

    @Override
    public void addQuads(IToolStackView tool, ModifierEntry entry, Function<SpriteIdentifier, Sprite> spriteGetter, AffineTransformation transforms, boolean isLarge, int startTintIndex, Consumer<Collection<BakedQuad>> quadConsumer, @Nullable ItemLayerPixels pixels) {
        // first, determine stored fluid
        // modifier must be tank
        FluidStack fluid = this.helper.getFluid(tool);
        // must have fluid
        if (!fluid.isEmpty()) {
            // must have texture for the proper state
            SpriteIdentifier template = this.getTemplate(tool, entry, fluid, isLarge);
            if (template != null) {
                // fluid properties
                IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(fluid.getFluid());
                Sprite fluidSprite = spriteGetter.apply(new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, attributes.getStillTexture(fluid)));

                // build fluid like the forge dynamic container model
                List<ModelElement> unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(-1, spriteGetter.apply(template)); // Use template as mask
                // TODO: is there anything that can be done about the fluid? to prevent weird offsets?
                List<BakedQuad> fluidQuads = UnbakedGeometryHelper.bakeElements(unbaked, mat -> fluidSprite, new SimpleModelState(transforms.applyOrigin(ORIGIN).compose(FluidContainerModel.FLUID_TRANSFORM), false), BAKE_LOCATION); // Bake with fluid texture

                // apply brightness and color
                int luminosity = fluid.getFluid().getFluidType().getLightLevel(fluid);
                if (luminosity > 0) {
                    QuadTransformers.settingEmissivity(luminosity).processInPlace(fluidQuads);
                }
                int color = attributes.getTintColor(fluid);
                if (color != -1) {
                    ColoredBlockModel.applyColorQuadTransformer(color).processInPlace(fluidQuads);
                }
                quadConsumer.accept(fluidQuads);
            }
        }
        // add tank outline quads
        super.addQuads(tool, entry, spriteGetter, transforms, isLarge, startTintIndex, quadConsumer, pixels);
    }

    /**
     * Cache key for the model
     */
    private record FluidModifierCacheKey(Modifier modifier, Fluid fluid) {
    }

    public record Unbaked(ToolTankHelper helper) implements IUnbakedModifierModel {
        @Nullable
        @Override
        public IBakedModifierModel forTool(Function<String, SpriteIdentifier> smallGetter, Function<String, SpriteIdentifier> largeGetter) {
            SpriteIdentifier smallTexture = smallGetter.apply("");
            SpriteIdentifier largeTexture = largeGetter.apply("");
            SpriteIdentifier smallFluid = smallGetter.apply("_full");
            SpriteIdentifier largeFluid = largeGetter.apply("_full");
            if (smallTexture != null || largeTexture != null || smallFluid != null || largeFluid != null) {
                return new FluidModifierModel(this.helper, smallTexture, largeTexture, smallFluid, largeFluid);
            }
            return null;
        }
    }
}
