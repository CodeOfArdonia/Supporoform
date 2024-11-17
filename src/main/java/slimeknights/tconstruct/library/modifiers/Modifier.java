package slimeknights.tconstruct.library.modifiers;

import lombok.Getter;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierManager.ModifierRegistrationEvent;
import slimeknights.tconstruct.library.modifiers.util.ModifierLevelDisplay;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.Util;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Class representing both modifiers and traits. Acts as a storage container for {@link ModuleHook} modules, which are used to implement various modifier behaviors.
 *
 * @see ModifierHooks
 * @see #registerHooks(Builder)
 */
@SuppressWarnings("unused")
public class Modifier implements IHaveLoader, IdAwareObject {
    /**
     * Modifier random instance, use for chance based effects
     */
    protected static Random RANDOM = new Random();

    /**
     * Priority of modfiers by default
     */
    public static final int DEFAULT_PRIORITY = 100;

    /**
     * Registry name of this modifier, null before fully registered
     */
    private ModifierId id;

    /**
     * Cached key used for translations
     */
    @Nullable
    private String translationKey;
    /**
     * Cached text component for display names
     */
    @Nullable
    private Text displayName;
    /**
     * Cached text component for description
     */
    @Nullable
    protected List<Text> descriptionList;
    /**
     * Cached text component for description
     */
    @Nullable
    private Text description;
    /**
     * Map of all modifier hooks registered to this modifier
     */
    @Getter
    private final ModuleHookMap hooks;

    /**
     * Creates a new modifier using the given hook map
     */
    protected Modifier(ModuleHookMap hooks) {
        this.hooks = hooks;
    }

    /**
     * Creates a new instance using the hook builder
     */
    public Modifier() {
        Builder hookBuilder = ModuleHookMap.builder();
        registerHooks(hookBuilder);
        this.hooks = hookBuilder.build();
    }

    /**
     * Registers a hook to the modifier.
     * Note that this is run in the constructor, so you are unable to use any instance fields in this method unless initialized in this method.
     * TODO 1.19: consider making abstract as everyone is going to need it in the future.
     */
    protected void registerHooks(Builder hookBuilder) {
    }

    @Override
    public IGenericLoader<? extends Modifier> getLoader() {
        throw new IllegalStateException("Attempting to serialize an unserializable modifier. This likely means the modifier did not override getLoader()");
    }

    /**
     * Override this method to make your modifier run earlier or later.
     * Higher numbers run earlier, 100 is default
     *
     * @return Priority
     */
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }


    /* Registry methods */

    /**
     * Sets the modifiers ID. Internal as ID is set through {@link ModifierRegistrationEvent} or the dynamic loader
     */
    final void setId(ModifierId name) {
        if (id != null) {
            throw new IllegalStateException("Attempted to set registry name with existing registry name! New: " + name + " Old: " + id);
        }
        this.id = name;
    }

    @Override
    public ModifierId getId() {
        return Objects.requireNonNull(id, "Modifier has null registry name");
    }

    /**
     * Checks if the modifier is in the given tag
     */
    public final boolean is(TagKey<Modifier> tag) {
        return ModifierManager.isInTag(this.getId(), tag);
    }


    /* Tooltips */

    /**
     * Called on pack reload to clear caches
     *
     * @param packType type of pack being reloaded
     */
    public void clearCache(ResourceType packType) {
        if (packType == ResourceType.CLIENT_RESOURCES) {
            displayName = null;
        }
    }

    /**
     * Gets the color for this modifier
     */
    public final TextColor getTextColor() {
        return ResourceColorManager.getTextColor(getTranslationKey());
    }

    /**
     * Gets the color for this modifier
     */
    public final int getColor() {
        return getTextColor().getRgb();
    }

    /**
     * Overridable method to create a translation key. Will be called once and the result cached
     *
     * @return Translation key
     */
    protected String makeTranslationKey() {
        return Util.makeTranslationKey("modifier", Objects.requireNonNull(id));
    }

    /**
     * Gets the translation key for this modifier
     *
     * @return Translation key
     */
    public final String getTranslationKey() {
        if (translationKey == null) {
            translationKey = makeTranslationKey();
        }
        return translationKey;
    }

    /**
     * Overridable method to create the display name for this modifier, ideal to modify colors.
     * TODO: this method does not really seem to do much, is it really needed? I feel like it was supposed to be called in {@link #getDisplayName()}, but it needs to be mutable for that.
     *
     * @return Display name
     */
    protected Text makeDisplayName() {
        return Text.translatable(getTranslationKey());
    }

    /**
     * Applies relevant text styles (typically color) to the modifier text
     *
     * @param component Component to modifiy
     * @return Resulting component
     */
    public MutableText applyStyle(MutableText component) {
        return component.styled(style -> style.withColor(getTextColor()));
    }

    /**
     * Gets the display name for this modifier
     *
     * @return Display name for this modifier
     */
    public Text getDisplayName() {
        if (displayName == null) {
            displayName = Text.translatable(getTranslationKey()).styled(style -> style.withColor(getTextColor()));
        }
        return displayName;
    }

    /**
     * Gets the display name for the given level of this modifier
     *
     * @param level Modifier level
     * @return Display name
     */
    public Text getDisplayName(int level) {
        return ModifierLevelDisplay.DEFAULT.nameForLevel(this, level);
    }

    /**
     * Stack sensitive version of {@link #getDisplayName(int)}. Useful for displaying persistent data such as overslime or redstone amount
     *
     * @param tool  Tool instance
     * @param entry Tool level
     * @return Stack sensitive display name
     */
    public Text getDisplayName(IToolStackView tool, ModifierEntry entry) {
        return entry.getDisplayName();
    }

    /**
     * Gets the description for this modifier
     *
     * @return Description for this modifier
     */
    public List<Text> getDescriptionList() {
        if (descriptionList == null) {
            descriptionList = Arrays.asList(
                    Text.translatable(getTranslationKey() + ".flavor").formatted(Formatting.ITALIC),
                    Text.translatable(getTranslationKey() + ".description"));
        }
        return descriptionList;
    }

    /**
     * Gets the description for this modifier, sensitive to the tool
     *
     * @param level Modifier level
     * @return Description for this modifier
     */
    public List<Text> getDescriptionList(int level) {
        return getDescriptionList();
    }

    /**
     * Gets the description for this modifier, sensitive to the tool
     *
     * @param tool  Tool containing this modifier
     * @param entry Modifier level
     * @return Description for this modifier
     */
    public List<Text> getDescriptionList(IToolStackView tool, ModifierEntry entry) {
        return getDescriptionList(entry.getLevel());
    }

    /**
     * Converts a list of text components to a single text component, newline separated
     */
    private static Text listToComponent(List<Text> list) {
        if (list.isEmpty()) {
            return Text.empty();
        }
        MutableText textComponent = Text.literal("");
        Iterator<Text> iterator = list.iterator();
        textComponent.append(iterator.next());
        while (iterator.hasNext()) {
            textComponent.append("\n");
            textComponent.append(iterator.next());
        }
        return textComponent;
    }

    /**
     * Gets the description for this modifier
     *
     * @return Description for this modifier
     */
    public final Text getDescription() {
        if (description == null) {
            description = listToComponent(getDescriptionList());
        }
        return description;
    }

    /**
     * Gets the description for this modifier
     *
     * @return Description for this modifier
     */
    public final Text getDescription(int level) {
        // if the method is not overridden, use the cached description component
        List<Text> extendedDescription = getDescriptionList(level);
        if (extendedDescription == getDescriptionList()) {
            return getDescription();
        }
        return listToComponent(extendedDescription);
    }

    /**
     * Gets the description for this modifier
     *
     * @return Description for this modifier
     */
    public final Text getDescription(IToolStackView tool, ModifierEntry entry) {
        // if the method is not overridden, use the cached description component
        List<Text> extendedDescription = getDescriptionList(tool, entry);
        if (extendedDescription == getDescriptionList()) {
            return getDescription();
        }
        return listToComponent(extendedDescription);
    }


    /* General hooks */

    /**
     * Determines if the modifier should display
     *
     * @param advanced If true, in an advanced view such as the tinker station. False for tooltips
     * @return True if the modifier should show
     */
    public boolean shouldDisplay(boolean advanced) {
        return true;
    }


    /* Hooks */


    /* Modules */

    /**
     * Gets a hook of this modifier. To modify the return values, use {@link #registerHooks(Builder)}
     *
     * @param hook Hook to fetch
     * @param <T>  Hook return type
     * @return Submodule implementing the hook, or default instance if its not implemented
     */
    public final <T> T getHook(ModuleHook<T> hook) {
        return this.hooks.getOrDefault(hook);
    }


    @Override
    public String toString() {
        return "Modifier{" + this.id + '}';
    }


    /* Utils */

    /**
     * Gets the tool stack from the given entities mainhand. Useful for specialized event handling in modifiers
     *
     * @param living Entity instance
     * @return Tool stack
     */
    @Nullable
    public static ToolStack getHeldTool(@Nullable LivingEntity living, Hand hand) {
        return getHeldTool(living, Util.getSlotType(hand));
    }

    /**
     * Gets the tool stack from the given entities mainhand. Useful for specialized event handling in modifiers
     *
     * @param living Entity instance
     * @return Tool stack
     */
    @Nullable
    public static ToolStack getHeldTool(@Nullable LivingEntity living, EquipmentSlot slot) {
        if (living == null) {
            return null;
        }
        ItemStack stack = living.getEquippedStack(slot);
        if (stack.isEmpty() || !stack.isIn(TinkerTags.Items.MODIFIABLE)) {
            return null;
        }
        ToolStack tool = ToolStack.from(stack);
        return tool.isBroken() ? null : tool;
    }

    /**
     * Gets the mining speed modifier for the current conditions, notably potions and armor enchants
     *
     * @param entity Entity to check
     * @return Mining speed modifier
     */
    public static float getMiningModifier(LivingEntity entity) {
        float modifier = 1.0f;
        // haste effect
        if (StatusEffectUtil.hasHaste(entity)) {
            modifier *= 1.0F + (StatusEffectUtil.getHasteAmplifier(entity) + 1) * 0.2f;
        }
        // mining fatigue
        StatusEffectInstance miningFatigue = entity.getStatusEffect(StatusEffects.MINING_FATIGUE);
        if (miningFatigue != null) {
            switch (miningFatigue.getAmplifier()) {
                case 0 -> modifier *= 0.3F;
                case 1 -> modifier *= 0.09F;
                case 2 -> modifier *= 0.0027F;
                default -> modifier *= 8.1E-4F;
            }
        }
        // water
        if (entity.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(entity)) {
            modifier /= 5.0F;
        }
        if (!entity.isOnGround()) {
            modifier /= 5.0F;
        }
        return modifier;
    }
}