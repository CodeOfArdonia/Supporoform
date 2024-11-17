package slimeknights.mantle.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fabricators_of_create.porting_lib.loot.IGlobalLootModifier;
import io.github.fabricators_of_create.porting_lib.loot.LootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionTypes;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.data.MantleCodecs;
import slimeknights.mantle.loot.condition.ILootModifierCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Loot modifier to inject an additional loot entry into an existing table
 */
public class AddEntryLootModifier extends LootModifier {
    public static final Codec<AddEntryLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst).and(inst.group(
            ILootModifierCondition.CODEC.listOf().fieldOf("post_conditions").forGetter(m -> m.modifierConditions),
            MantleCodecs.LOOT_ENTRY.fieldOf("entry").forGetter(m -> m.entry),
            MantleCodecs.LOOT_FUNCTIONS.fieldOf("functions").forGetter(m -> m.functions))).apply(inst, AddEntryLootModifier::new));

    /**
     * Additional conditions that can consider the previously generated loot
     */
    private final List<ILootModifierCondition> modifierConditions;
    /**
     * Entry for generating loot
     */
    private final LootPoolEntry entry;
    /**
     * Functions to apply to the entry, allows adding functions to parented loot entries such as alternatives
     */
    private final LootFunction[] functions;
    /**
     * Functions merged into a single function for ease of use
     */
    private final BiFunction<ItemStack, LootContext, ItemStack> combinedFunctions;

    protected AddEntryLootModifier(LootCondition[] conditionsIn, List<ILootModifierCondition> modifierConditions, LootPoolEntry entry, LootFunction[] functions) {
        super(conditionsIn);
        this.modifierConditions = modifierConditions;
        this.entry = entry;
        this.functions = functions;
        this.combinedFunctions = LootFunctionTypes.join(functions);
    }

    /**
     * Creates a builder for this loot modifier
     */
    public static Builder builder(LootPoolEntry entry) {
        return new Builder(entry);
    }

    /**
     * Creates a builder for this loot modifier
     */
    public static Builder builder(LootPoolEntry.Builder<?> builder) {
        return builder(builder.build());
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // if any condition fails, exit immediately
        for (ILootModifierCondition modifierCondition : this.modifierConditions) {
            if (!modifierCondition.test(generatedLoot, context)) {
                return generatedLoot;
            }
        }
        // generate the actual entry
        Consumer<ItemStack> consumer = LootFunction.apply(this.combinedFunctions, generatedLoot::add, context);
        this.entry.expand(context, generator -> generator.generateLoot(consumer, context));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    /**
     * Builder for a conditional loot entry
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder extends AbstractLootModifierBuilder<Builder> {
        private final List<ILootModifierCondition> modifierConditions = new ArrayList<>();
        private final LootPoolEntry entry;
        private final List<LootFunction> functions = new ArrayList<>();

        /**
         * Adds a loot entry condition to the builder
         */
        public Builder addCondition(ILootModifierCondition condition) {
            this.modifierConditions.add(condition);
            return this;
        }

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
        public AddEntryLootModifier build() {
            return new AddEntryLootModifier(this.getConditions(), this.modifierConditions, this.entry, this.functions.toArray(new LootFunction[0]));
        }
    }
}
