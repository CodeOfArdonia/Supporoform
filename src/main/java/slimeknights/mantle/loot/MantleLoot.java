package slimeknights.mantle.loot;

import com.google.gson.JsonDeserializer;
import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.JsonSerializer;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.RegisterEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.loot.condition.*;
import slimeknights.mantle.loot.function.RetexturedLootFunction;
import slimeknights.mantle.loot.function.SetFluidLootFunction;
import slimeknights.mantle.registration.adapter.RegistryAdapter;

import java.util.Objects;

import static slimeknights.mantle.loot.condition.ILootModifierCondition.MODIFIER_CONDITIONS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MantleLoot {
    /**
     * Condition to match a block tag and property predicate
     */
    public static LootConditionType BLOCK_TAG_CONDITION;
    /**
     * Function to add block entity texture to a dropped item
     */
    public static LootFunctionType RETEXTURED_FUNCTION;
    /**
     * Function to add a fluid to an item fluid capability
     */
    public static LootFunctionType SET_FLUID_FUNCTION;

    /**
     * Called during serializer registration to register any relevant loot logic
     */
    public static void registerGlobalLootModifiers(final RegisterEvent event) {
        RegistryAdapter<Codec<? extends IGlobalLootModifier>> adapter = new RegistryAdapter<>(Objects.requireNonNull(event.getForgeRegistry()));
        adapter.register(AddEntryLootModifier.CODEC, "add_entry");
        adapter.register(ReplaceItemLootModifier.CODEC, "replace_item");

        // functions
        RETEXTURED_FUNCTION = registerFunction("fill_retextured_block", RetexturedLootFunction.SERIALIZER);
        SET_FLUID_FUNCTION = registerFunction("set_fluid", SetFluidLootFunction.SERIALIZER);

        // conditions
        BLOCK_TAG_CONDITION = Registry.register(Registries.LOOT_CONDITION_TYPE, Mantle.getResource("block_tag"), new LootConditionType(BlockTagLootCondition.SERIALIZER));

        // loot modifier conditions
        MODIFIER_CONDITIONS.registerDeserializer(InvertedModifierLootCondition.ID, (JsonDeserializer<? extends ILootModifierCondition>) InvertedModifierLootCondition::deserialize);
        MODIFIER_CONDITIONS.registerDeserializer(EmptyModifierLootCondition.ID, EmptyModifierLootCondition.INSTANCE);
        MODIFIER_CONDITIONS.registerDeserializer(ContainsItemModifierLootCondition.ID, (JsonDeserializer<? extends ILootModifierCondition>) ContainsItemModifierLootCondition::deserialize);
    }

    /**
     * Registers a loot function
     *
     * @param name       Loot function name
     * @param serializer Loot function serializer
     * @return Registered loot function
     */
    private static LootFunctionType registerFunction(String name, JsonSerializer<? extends LootFunction> serializer) {
        return Registry.register(Registries.LOOT_FUNCTION_TYPE, Mantle.getResource(name), new LootFunctionType(serializer));
    }
}
