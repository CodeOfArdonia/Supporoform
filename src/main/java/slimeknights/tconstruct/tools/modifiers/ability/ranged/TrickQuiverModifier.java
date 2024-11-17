package slimeknights.tconstruct.tools.modifiers.ability.ranged;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.dynamic.InventoryMenuModifier;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.ranged.BowAmmoModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.nbt.INamespacedNBTView;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.function.Predicate;

public class TrickQuiverModifier extends InventoryMenuModifier implements BowAmmoModifierHook {
    private static final Identifier INVENTORY_KEY = TConstruct.getResource("trick_quiver");
    private static final Identifier SELECTED_SLOT = TConstruct.getResource("trick_quiver_selected");
    private static final Pattern TRICK_ARROW = new Pattern(TConstruct.getResource("tipped_arrow"));
    /**
     * Message when disabling the trick quiver
     */
    private static final Text DISABLED = TConstruct.makeTranslation("modifier", "trick_quiver.disabled");
    /**
     * Message displayed when the selected slot is empty
     */
    private static final String EMPTY = TConstruct.makeTranslationKey("modifier", "trick_quiver.empty");
    /**
     * Message to display selected slot
     */
    private static final String SELECTED = TConstruct.makeTranslationKey("modifier", "trick_quiver.selected");

    public TrickQuiverModifier() {
        super(INVENTORY_KEY, 3);
    }

    @Override
    public int getPriority() {
        return 70; // run after interaction modifiers, but before crystal shot
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.BOW_AMMO);
    }

    @Override
    public int getSlots(INamespacedNBTView tool, ModifierEntry modifier) {
        return 3;
    }

    @Override
    public int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
        return modifier.getLevel() == 1 ? 32 : 64;
    }

    @Override
    public boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
        Item item = stack.getItem();
        return (item == Items.FIREWORK_ROCKET && tool.hasTag(TinkerTags.Items.CROSSBOWS)) || stack.getItem() instanceof ArrowItem;
    }

    @Nullable
    @Override
    public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
        return hasStack ? null : TRICK_ARROW;
    }

    @Override
    public ItemStack findAmmo(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack standardAmmo, Predicate<ItemStack> ammoPredicate) {
        // if selected is too big (disabled), will automatially return nothing
        return this.getStack(tool, modifier, tool.getPersistentData().getInt(SELECTED_SLOT));
    }

    @Override
    public void shrinkAmmo(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack ammo, int needed) {
        // assume no one else touched our selected slot, good assumption
        ammo.decrement(needed);
        this.setStack(tool, modifier, tool.getPersistentData().getInt(SELECTED_SLOT), ammo);
    }

    @Override
    public ActionResult onToolUse(IToolStackView tool, ModifierEntry modifier, PlayerEntity player, Hand hand, InteractionSource source) {
        if (!player.isInSneakingPose()) {
            if (!player.getWorld().isClient) {
                // first, increment the number
                ModDataNBT data = tool.getPersistentData();
                int totalSlots = this.getSlots(tool, modifier);
                // support going 1 above max to disable the trick arrows
                int newSelected = (data.getInt(SELECTED_SLOT) + 1) % (totalSlots + 1);
                data.putInt(SELECTED_SLOT, newSelected);

                // display a message about what is now selected
                if (newSelected == totalSlots) {
                    player.sendMessage(DISABLED, true);
                } else {
                    ItemStack selectedStack = this.getStack(tool, modifier, newSelected);
                    if (selectedStack.isEmpty()) {
                        player.sendMessage(Text.translatable(EMPTY, newSelected + 1), true);
                    } else {
                        player.sendMessage(Text.translatable(SELECTED, selectedStack.getName(), newSelected + 1), true);
                    }
                }
            }
            return ActionResult.SUCCESS;
        }
        return super.onToolUse(tool, modifier, player, hand, source);
    }
}
