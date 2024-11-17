package slimeknights.tconstruct.library.tools.nbt;


import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.stat.INumericToolStat;

/**
 * Provides mostly read only access to {@link ToolStack}.
 * Used since modifiers should not be modifying the tool materials or modifiers in their behaviors.
 * If you receive an instance of this interface a parameter, do NOT use an instanceof check and cast it to a ToolStack. Don't make me use a private wrapper class.
 */
public interface IToolStackView extends IToolContext {
    /* Stats */

    /**
     * On built tools, contains the full tool stats. During tool rebuild, contains the base stats before considering modifiers.
     */
    StatsNBT getStats();

    /**
     * Gets the tool stats if parsed, or parses from NBT if not yet parsed
     *
     * @return stats
     */
    MultiplierNBT getMultipliers();

    /**
     * Commonly used operation, getting a stat multiplier
     */
    default float getMultiplier(INumericToolStat<?> stat) {
        return this.getMultipliers().get(stat);
    }


    /* Damage state */

    /**
     * Gets the current damage of the tool
     */
    int getDamage();

    /**
     * Gets the current durability remaining for this tool
     */
    int getCurrentDurability();

    /**
     * Checks whether the tool is broken
     */
    boolean isBroken();

    /**
     * If true, tool is marked unbreakable by vanilla
     */
    boolean isUnbreakable();

    /**
     * Sets the tools current damage.
     * Note in general you should use {@link ToolDamageUtil#damage(IToolStackView, int, LivingEntity, ItemStack)} or {@link ToolDamageUtil#repair(IToolStackView, int)} as they handle modifiers
     *
     * @param damage New damage
     */
    void setDamage(int damage);

    /**
     * Gets persistent modifier data from the tool.
     * This data may be edited by modifiers and will persist when stats rebuild
     * TODO 1.19: change return type to NamespaceNBT as modifiers should not be changing slots, will make bow hooks easier
     */
    @Override
    ModDataNBT getPersistentData();

    /**
     * Gets volatile modifier data from the tool.
     * This data will be reset whenever modifiers reload and should not be edited.
     * TODO 1.19: change return type to INamespacedNBTView as modifiers should not be slot sensitive
     */
    IModDataView getVolatileData();


    /* Helpers */

    /**
     * Gets the free upgrade slots remaining on the tool
     *
     * @return Free upgrade slots
     */
    default int getFreeSlots(SlotType type) {
        return this.getPersistentData().getSlots(type) + this.getVolatileData().getSlots(type);
    }
}