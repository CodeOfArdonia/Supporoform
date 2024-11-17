package slimeknights.mantle.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fabricators_of_create.porting_lib.loot.IGlobalLootModifier;
import io.github.fabricators_of_create.porting_lib.loot.LootModifier;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.recipe.Ingredient;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.data.MantleCodecs;
import slimeknights.mantle.recipe.helper.ItemOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiFunction;

/**
 * Loot modifier to replace an item with another
 */
public class ReplaceItemLootModifier extends LootModifier {
    public static final Codec<ReplaceItemLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst).and(
            inst.group(
                    MantleCodecs.INGREDIENT.fieldOf("original").forGetter(m -> m.original),
                    ItemOutput.REQUIRED_STACK_CODEC.fieldOf("replacement").forGetter(m -> m.replacement),
                    MantleCodecs.LOOT_FUNCTIONS.fieldOf("functions").forGetter(m -> m.functions)
            )).apply(inst, ReplaceItemLootModifier::new));

    /**
     * Ingredient to test for the original item
     */
    private final Ingredient original;
    /**
     * Item for the replacement
     */
    private final ItemOutput replacement;
    /**
     * Functions to apply to the replacement
     */
    private final LootFunction[] functions;
    /**
     * Functions merged into a single function for ease of use
     */
    private final BiFunction<ItemStack, LootContext, ItemStack> combinedFunctions;

    protected ReplaceItemLootModifier(LootCondition[] conditionsIn, Ingredient original, ItemOutput replacement, LootFunction[] functions) {
        super(conditionsIn);
        this.original = original;
        this.replacement = replacement;
        this.functions = functions;
        this.combinedFunctions = LootFunctionTypes.join(functions);
    }

    /**
     * Creates a builder to create a loot modifier
     */
    public static Builder builder(Ingredient original, ItemOutput replacement) {
        return new Builder(original, replacement);
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ListIterator<ItemStack> iterator = generatedLoot.listIterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (this.original.test(stack)) {
                ItemStack replacement = this.replacement.get();
                iterator.set(this.combinedFunctions.apply(ItemHandlerHelper.copyStackWithSize(replacement, replacement.getCount() * stack.getCount()), context));
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    /**
     * Logic to build this modifier for datagen
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder extends AbstractLootModifierBuilder<Builder> {
        private final Ingredient input;
        private final ItemOutput replacement;
        private final List<LootFunction> functions = new ArrayList<>();

        /**
         * Adds a loot function to the builder
         */
        public Builder addFunction(LootFunction function) {
            this.functions.add(function);
            return this;
        }

        /**
         * Builds the final modifier
         */
        public ReplaceItemLootModifier build() {
            return new ReplaceItemLootModifier(this.getConditions(), this.input, this.replacement, this.functions.toArray(new LootFunction[0]));
        }
    }
}
