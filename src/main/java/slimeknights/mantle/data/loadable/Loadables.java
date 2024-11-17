package slimeknights.mantle.data.loadable;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import slimeknights.mantle.data.loadable.common.RegistryLoadable;
import slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;

import java.util.function.BiFunction;

/**
 * Various loadable instances provided by this mod
 */
@SuppressWarnings({"deprecation", "unused"})
public class Loadables {
    private Loadables() {
    }

    /**
     * Loadable for a resource location
     */
    public static final StringLoadable<Identifier> RESOURCE_LOCATION = StringLoadable.DEFAULT.xmap((s, e) -> {
        try {
            return new Identifier(s);
        } catch (InvalidIdentifierException ex) {
            throw e.create(ex);
        }
    }, (r, e) -> r.toString());
    public static final StringLoadable<ToolAction> TOOL_ACTION = StringLoadable.DEFAULT.flatXmap(ToolAction::get, ToolAction::name);

    /* Registries */
    public static final ResourceLocationLoadable<SoundEvent> SOUND_EVENT = new RegistryLoadable<>(Registries.SOUND_EVENT);
    public static final ResourceLocationLoadable<Fluid> FLUID = new RegistryLoadable<>(Registries.FLUID);
    public static final ResourceLocationLoadable<StatusEffect> MOB_EFFECT = new RegistryLoadable<>(Registries.STATUS_EFFECT);
    public static final ResourceLocationLoadable<Block> BLOCK = new RegistryLoadable<>(Registries.BLOCK);
    public static final ResourceLocationLoadable<Enchantment> ENCHANTMENT = new RegistryLoadable<>(Registries.ENCHANTMENT);
    public static final ResourceLocationLoadable<EntityType<?>> ENTITY_TYPE = new RegistryLoadable<>(Registries.ENTITY_TYPE);
    public static final ResourceLocationLoadable<Item> ITEM = new RegistryLoadable<>(Registries.ITEM);
    public static final ResourceLocationLoadable<Potion> POTION = new RegistryLoadable<>(Registries.POTION);
    public static final ResourceLocationLoadable<ParticleType<?>> PARTICLE_TYPE = new RegistryLoadable<>(Registries.PARTICLE_TYPE);
    public static final ResourceLocationLoadable<BlockEntityType<?>> BLOCK_ENTITY_TYPE = new RegistryLoadable<>(Registries.BLOCK_ENTITY_TYPE);
    public static final ResourceLocationLoadable<EntityAttribute> ATTRIBUTE = new RegistryLoadable<>(Registries.ATTRIBUTE);

    /* Non-default registries */
    public static final StringLoadable<Fluid> NON_EMPTY_FLUID = notValue(FLUID, Fluids.EMPTY, "Fluid cannot be empty");
    public static final StringLoadable<Block> NON_EMPTY_BLOCK = notValue(BLOCK, Blocks.AIR, "Block cannot be air");
    public static final StringLoadable<Item> NON_EMPTY_ITEM = notValue(ITEM, Items.AIR, "Item cannot be empty");

    /* Tag keys */
    public static final StringLoadable<TagKey<Fluid>> FLUID_TAG = tagKey(RegistryKeys.FLUID);
    public static final StringLoadable<TagKey<StatusEffect>> MOB_EFFECT_TAG = tagKey(RegistryKeys.STATUS_EFFECT);
    public static final StringLoadable<TagKey<Block>> BLOCK_TAG = tagKey(RegistryKeys.BLOCK);
    public static final StringLoadable<TagKey<Enchantment>> ENCHANTMENT_TAG = tagKey(RegistryKeys.ENCHANTMENT);
    public static final StringLoadable<TagKey<EntityType<?>>> ENTITY_TYPE_TAG = tagKey(RegistryKeys.ENTITY_TYPE);
    public static final StringLoadable<TagKey<Item>> ITEM_TAG = tagKey(RegistryKeys.ITEM);
    public static final StringLoadable<TagKey<Potion>> POTION_TAG = tagKey(RegistryKeys.POTION);
    public static final StringLoadable<TagKey<BlockEntityType<?>>> BLOCK_ENTITY_TYPE_TAG = tagKey(RegistryKeys.BLOCK_ENTITY_TYPE);
    public static final StringLoadable<TagKey<DamageType>> DAMAGE_TYPE_TAG = tagKey(RegistryKeys.DAMAGE_TYPE);


    /* Helpers */

    /**
     * Creates a tag key loadable
     */
    public static <T> StringLoadable<TagKey<T>> tagKey(RegistryKey<? extends Registry<T>> registry) {
        return RESOURCE_LOCATION.flatXmap(key -> TagKey.of(registry, key), TagKey::id);
    }

    /**
     * Maps a loadable to a variant that disallows a particular value
     */
    public static <T> StringLoadable<T> notValue(StringLoadable<T> loadable, T notValue, String errorMsg) {
        BiFunction<T, ErrorFactory, T> mapper = (value, error) -> {
            if (value == notValue) {
                throw error.create(errorMsg);
            }
            return value;
        };
        return loadable.xmap(mapper, mapper);
    }
}
