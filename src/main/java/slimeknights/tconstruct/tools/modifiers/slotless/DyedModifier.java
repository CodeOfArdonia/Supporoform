package slimeknights.tconstruct.tools.modifiers.slotless;

import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

public class DyedModifier extends NoLevelsModifier implements ModifierRemovalHook {
    private static final String FORMAT_KEY = TConstruct.makeTranslationKey("modifier", "dyed.formatted");

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.REMOVE);
    }

    @Override
    public Text getDisplayName(IToolStackView tool, ModifierEntry entry) {
        ModDataNBT persistentData = tool.getPersistentData();
        Identifier key = this.getId();
        if (persistentData.contains(key, NbtElement.INT_TYPE)) {
            int color = persistentData.getInt(key);
            return this.applyStyle(Text.translatable(FORMAT_KEY, String.format("%06X", color)));
        }
        return super.getDisplayName();
    }

    @Nullable
    @Override
    public Text onRemoved(IToolStackView tool, Modifier modifier) {
        tool.getPersistentData().remove(this.getId());
        return null;
    }
}
