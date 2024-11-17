package slimeknights.tconstruct.shared.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import slimeknights.mantle.item.LecternBookItem;
import slimeknights.tconstruct.library.client.book.TinkerBook;

public class TinkerBookItem extends LecternBookItem {
    private final BookType bookType;

    public TinkerBookItem(Settings props, BookType bookType) {
        super(props);
        this.bookType = bookType;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            TinkerBook.getBook(this.bookType).openGui(hand, stack);
        }
        return new TypedActionResult<>(ActionResult.SUCCESS, stack);
    }

    @Override
    public void openLecternScreenClient(BlockPos pos, ItemStack stack) {
        TinkerBook.getBook(this.bookType).openGui(pos, stack);
    }

    /**
     * Simple enum to allow selecting the book on the client
     */
    public enum BookType {
        MATERIALS_AND_YOU,
        PUNY_SMELTING,
        MIGHTY_SMELTING,
        TINKERS_GADGETRY,
        FANTASTIC_FOUNDRY,
        ENCYCLOPEDIA
    }
}
