package slimeknights.tconstruct.world.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.VineBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.item.TooltipItem;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.block.DirtType;
import slimeknights.tconstruct.world.block.FoliageType;
import slimeknights.tconstruct.world.block.SlimeVineBlock;
import slimeknights.tconstruct.world.block.SlimeVineBlock.VineStage;

public class SlimeGrassSeedItem extends TooltipItem {
    private final FoliageType foliage;

    public SlimeGrassSeedItem(Settings properties, FoliageType foliage) {
        super(properties);
        this.foliage = foliage;
    }

    /**
     * Gets the slime type for the given block
     */
    @Nullable
    private static DirtType getDirtType(Block block) {
        for (DirtType type : DirtType.values()) {
            if (TinkerWorld.allDirt.get(type) == block) {
                return type;
            }
        }
        return null;
    }

    /**
     * Gets the vines associated with these seeds
     */
    @Nullable
    private Block getVines() {
        return switch (this.foliage) {
            case SKY -> TinkerWorld.skySlimeVine.get();
            case ENDER -> TinkerWorld.enderSlimeVine.get();
            default -> null;
        };
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos pos = context.getBlockPos();
        World world = context.getWorld();
        BlockState state = world.getBlockState(pos);
        BlockState newState = null;

        // try vines first
        if (state.getBlock() == Blocks.VINE) {
            Block slimyVines = this.getVines();
            if (slimyVines != null) {
                // copy over the directions
                newState = slimyVines.getDefaultState().with(SlimeVineBlock.STAGE, VineStage.START);
                for (BooleanProperty prop : VineBlock.FACING_PROPERTIES.values()) {
                    if (state.get(prop)) {
                        newState = newState.with(prop, true);
                    }
                }
            }
        }

        // if vines did not succeed, try grass
        if (newState == null) {
            DirtType type = getDirtType(state.getBlock());
            if (type != null) {
                newState = TinkerWorld.slimeGrass.get(type).get(this.foliage).getDefaultState();
            } else {
                return ActionResult.PASS;
            }
        }

        // will have a state at this point
        if (!world.isClient) {
            world.setBlockState(pos, newState);
            world.playSound(null, pos, newState.getSoundGroup(world, pos, context.getPlayer()).getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
            PlayerEntity player = context.getPlayer();
            if (player == null || !player.isCreative()) {
                context.getStack().decrement(1);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void fillItemCategory(ItemGroup group, DefaultedList<ItemStack> items) {
        if (this.foliage != FoliageType.ICHOR) {
            super.fillItemCategory(group, items);
        }
    }
}
