package slimeknights.tconstruct.library.tools.item;

import net.minecraft.item.ItemConvertible;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/**
 * Base interface for all tools that can receive modifiers
 */
public interface IModifiable extends ItemConvertible {
    /**
     * Modifier key to make a tool spawn an indestructable entity
     */
    Identifier INDESTRUCTIBLE_ENTITY = TConstruct.getResource("indestructible");
    /**
     * Modifier key to make a tool spawn an indestructable entity
     */
    Identifier SHINY = TConstruct.getResource("shiny");
    /**
     * Modifier key to make a tool spawn an indestructable entity
     */
    Identifier RARITY = TConstruct.getResource("rarity");
    /**
     * Modifier key to defer tool interaction to the offhand if present
     */
    Identifier DEFER_OFFHAND = TConstruct.getResource("defer_offhand");
    /**
     * Modifier key to entirely disable tool interaction
     */
    Identifier NO_INTERACTION = TConstruct.getResource("no_interaction");

    /**
     * Gets the definition of this tool for building and applying modifiers
     */
    ToolDefinition getToolDefinition();

    /**
     * Sets the rarity of the stack
     *
     * @param volatileData NBT
     * @param rarity       Rarity, only supports vanilla values
     */
    static void setRarity(ModDataNBT volatileData, Rarity rarity) {
        int current = volatileData.getInt(RARITY);
        if (rarity.ordinal() > current) {
            volatileData.putInt(RARITY, rarity.ordinal());
        }
    }
}
