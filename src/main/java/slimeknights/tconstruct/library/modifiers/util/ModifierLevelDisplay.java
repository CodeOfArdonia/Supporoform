package slimeknights.tconstruct.library.modifiers.util;

import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.DefaultingLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.SingletonLoader;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.utils.RomanNumeralHelper;

import java.util.function.BiFunction;

import net.minecraft.text.Text;

public interface ModifierLevelDisplay extends IHaveLoader {
    /**
     * Default display, listing name followed by a roman numeral for level
     */
    ModifierLevelDisplay DEFAULT = simple((modifier, level) ->
            modifier.applyStyle(Text.translatable(modifier.getTranslationKey())
                    .append(" ")
                    .append(RomanNumeralHelper.getNumeral(level))));

    /**
     * Loader instance
     */
    DefaultingLoaderRegistry<ModifierLevelDisplay> LOADER = new DefaultingLoaderRegistry<>("Modifier Level Display", DEFAULT, true);

    /**
     * Gets the name for a modifier for the given level
     */
    Text nameForLevel(Modifier modifier, int level);

    @Override
    IGenericLoader<? extends ModifierLevelDisplay> getLoader();


    /* Non-default implementations */

    /**
     * Displays just the name, for modifiers where multiple levels has no effect
     */
    ModifierLevelDisplay NO_LEVELS = simple((modifier, level) -> modifier.getDisplayName());

    /**
     * Displays just the name for the first level, for modifiers that can have multiple levels but don't by design
     */
    ModifierLevelDisplay SINGLE_LEVEL = simple((modifier, level) -> {
        if (level == 1) {
            return modifier.getDisplayName();
        }
        return DEFAULT.nameForLevel(modifier, level);
    });

    /**
     * Displays level with pluses instead of numbers
     */
    ModifierLevelDisplay PLUSES = simple((modifier, level) -> {
        if (level > 1) {
            return modifier.applyStyle(Text.translatable(modifier.getTranslationKey()).append("+".repeat(level - 1)));
        }
        return modifier.getDisplayName();
    });

    /**
     * Creates a simple level display singleton
     */
    static ModifierLevelDisplay simple(BiFunction<Modifier, Integer, Text> name) {
        return SingletonLoader.singleton(loader -> new ModifierLevelDisplay() {
            @Override
            public Text nameForLevel(Modifier modifier, int level) {
                return name.apply(modifier, level);
            }

            @Override
            public IGenericLoader<? extends ModifierLevelDisplay> getLoader() {
                return loader;
            }
        });
    }

    /**
     * Name that is unique for the first several levels
     *
     * @param unique      Number of levels with unique names
     * @param firstUnique If true, first level has a unique name. If false, first level uses the levelless modifier name
     */
    record UniqueForLevels(int unique, boolean firstUnique) implements ModifierLevelDisplay {
        public static final RecordLoadable<UniqueForLevels> LOADER = RecordLoadable.create(
                IntLoadable.FROM_ONE.requiredField("unique_until", UniqueForLevels::unique),
                BooleanLoadable.INSTANCE.defaultField("first_unique", false, UniqueForLevels::firstUnique),
                UniqueForLevels::new);

        public UniqueForLevels(int unique) {
            this(unique, false);
        }

        @Override
        public Text nameForLevel(Modifier modifier, int level) {
            if (!this.firstUnique && level == 1) {
                return modifier.getDisplayName();
            }
            if (level <= this.unique) {
                return modifier.applyStyle(Text.translatable(modifier.getTranslationKey() + "." + level));
            }
            return DEFAULT.nameForLevel(modifier, level);
        }

        @Override
        public IGenericLoader<? extends ModifierLevelDisplay> getLoader() {
            return LOADER;
        }
    }
}
