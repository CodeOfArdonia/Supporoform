package slimeknights.mantle.loot;

import io.github.fabricators_of_create.porting_lib.loot.LootModifier;
import lombok.RequiredArgsConstructor;
import net.minecraft.loot.condition.LootCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Base builder for a global loot modifier during datagen, intended to be used with {@link GlobalLootModifierProvider}
 */
public abstract class AbstractLootModifierBuilder<B extends AbstractLootModifierBuilder<B>> {
    private final List<LootCondition> conditions = new ArrayList<>();

    /**
     * Adds a condition to the builder
     */
    @SuppressWarnings("unchecked")
    public B addCondition(LootCondition condition) {
        this.conditions.add(condition);
        return (B) this;
    }

    /**
     * Gets the built list of conditions
     */
    protected LootCondition[] getConditions() {
        return this.conditions.toArray(new LootCondition[0]);
    }

    /**
     * Generic builder for a modifier that just takes conditions
     */
    @RequiredArgsConstructor
    public static class GenericLootModifierBuilder<M extends LootModifier> extends AbstractLootModifierBuilder<GenericLootModifierBuilder<M>> {
        private final Function<LootCondition[], M> constructor;

        /**
         * Builds the final instance
         */
        public M build() {
            return this.constructor.apply(this.getConditions());
        }
    }
}
