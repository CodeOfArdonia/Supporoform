package slimeknights.tconstruct.library.tools.nbt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Helper which contains a lazily loaded tool stack, used for recipe output to reduce NBT parsing
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LazyToolStack {
    @Nullable
    private ItemStack stack;
    @Nullable
    private ToolStack tool;
    @Getter
    private final int size;

    /**
     * Creates from a stack, lazily loading the tool stack
     */
    public static LazyToolStack from(ItemStack stack) {
        return new LazyToolStack(stack, null, stack.getCount());
    }

    /**
     * Creates from a tool with the given count, lazily loading the stack
     */
    public static LazyToolStack from(ToolStack tool, int count) {
        return new LazyToolStack(null, tool, count);
    }

    /**
     * Creates from a tool with size 1, lazily loading the stack
     */
    public static LazyToolStack from(ToolStack tool) {
        return from(tool, 1);
    }

    /**
     * Gets the tool for this instance
     */
    public ToolStack getTool() {
        if (this.tool == null) {
            assert this.stack != null;
            this.tool = ToolStack.from(this.stack);
        }
        return this.tool;
    }

    /**
     * Gets the item stack for this instance
     */
    public ItemStack getStack() {
        if (this.stack == null) {
            assert this.tool != null;
            this.stack = this.tool.createStack(this.size);
        }
        return this.stack;
    }
}
