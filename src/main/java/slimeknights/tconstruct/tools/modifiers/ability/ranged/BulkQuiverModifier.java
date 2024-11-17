package slimeknights.tconstruct.tools.modifiers.ability.ranged;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.dynamic.InventoryMenuModifier;
import slimeknights.tconstruct.library.modifiers.hook.ranged.BowAmmoModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.function.Predicate;

public class BulkQuiverModifier extends InventoryMenuModifier implements BowAmmoModifierHook {
    private static final Identifier INVENTORY_KEY = TConstruct.getResource("bulk_quiver");
    private static final Identifier LAST_SLOT = TConstruct.getResource("quiver_last_selected");
    private static final Pattern ARROW = new Pattern(TConstruct.getResource("arrow"));

    public BulkQuiverModifier() {
        super(INVENTORY_KEY, 2);
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.BOW_AMMO);
    }

    @Override
    public int getPriority() {
        return 50; // after crystalshot
    }

    @Override
    public boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
        Item item = stack.getItem();
        return (item == Items.FIREWORK_ROCKET && tool.hasTag(TinkerTags.Items.CROSSBOWS)) || stack.getItem() instanceof ArrowItem;
    }

    @Nullable
    @Override
    public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
        return hasStack ? null : ARROW;
    }

    @Override
    public ItemStack findAmmo(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack standardAmmo, Predicate<ItemStack> ammoPredicate) {
        // skip if we have standard ammo, this quiver holds backup arrows
        if (!standardAmmo.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ModDataNBT persistentData = tool.getPersistentData();
        Identifier key = this.getInventoryKey();
        NbtList slots = persistentData.get(key, GET_COMPOUND_LIST);
        if (!slots.isEmpty()) {
            // search all slots for the first match
            for (int i = 0; i < slots.size(); i++) {
                NbtCompound compound = slots.getCompound(i);
                ItemStack stack = ItemStack.fromNbt(compound);
                if (!stack.isEmpty() && ammoPredicate.test(stack)) {
                    persistentData.putInt(LAST_SLOT, compound.getInt(TAG_SLOT));
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void shrinkAmmo(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ItemStack ammo, int needed) {
        // we assume no one else touched the quiver inventory, this is a good assumption, do not make it a bad assumption by modifying the quiver in other modifiers
        ammo.decrement(needed);
        this.setStack(tool, modifier, tool.getPersistentData().getInt((LAST_SLOT)), ammo);
    }
}
