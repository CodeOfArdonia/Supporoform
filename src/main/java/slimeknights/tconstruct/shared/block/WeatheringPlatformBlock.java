package slimeknights.tconstruct.shared.block;

import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import lombok.Getter;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.shared.TinkerCommons;

import java.util.Optional;

public class WeatheringPlatformBlock extends PlatformBlock implements Oxidizable {
    @Getter
    private final OxidationLevel age;

    public WeatheringPlatformBlock(OxidationLevel age, Settings props) {
        super(props);
        this.age = age;
    }

    @Override
    protected boolean verticalConnect(BlockState state) {
        return state.isIn(TinkerTags.Blocks.COPPER_PLATFORMS);
    }

    @Override
    public void randomTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
        this.tickDegradation(pState, pLevel, pPos, pRandom);
    }

    /**
     * Gets the next state for weathering
     */
    @Nullable
    private static OxidationLevel getNext(OxidationLevel original) {
        return switch (original) {
            case UNAFFECTED -> OxidationLevel.EXPOSED;
            case EXPOSED -> OxidationLevel.WEATHERED;
            case WEATHERED -> OxidationLevel.OXIDIZED;
            default -> null;
        };
    }

    @Override
    public boolean hasRandomTicks(BlockState pState) {
        return getNext(this.age) != null;
    }

    @Override
    public Optional<BlockState> getDegradationResult(BlockState state) {
        return Optional.ofNullable(getNext(this.age))
                .map(next -> TinkerCommons.copperPlatform.get(next).getStateWithProperties(state));
    }

    @Override
    public OxidationLevel getDegradationLevel() {
        return OxidationLevel.WEATHERED;
    }

    /**
     * Gets the next state for weathering
     */
    @Nullable
    private static OxidationLevel getPrevious(OxidationLevel original) {
        return switch (original) {
            case EXPOSED -> OxidationLevel.UNAFFECTED;
            case WEATHERED -> OxidationLevel.EXPOSED;
            case OXIDIZED -> OxidationLevel.WEATHERED;
            default -> null;
        };
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, ItemUsageContext context, ToolAction toolAction, boolean simulate) {
        if (ToolActions.AXE_SCRAPE.equals(toolAction)) {
            OxidationLevel prev = getPrevious(this.age);
            if (prev != null) {
                return TinkerCommons.copperPlatform.get(prev).getStateWithProperties(state);
            }
        }
        return null;
    }

    @Override
    public ActionResult onUse(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() == Items.HONEYCOMB) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
            }
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            level.setBlockState(pos, TinkerCommons.waxedCopperPlatform.get(this.age).getStateWithProperties(state), 11);
            level.syncWorldEvent(player, 3003, pos, 0);
            return ActionResult.success(level.isClient);
        }
        return ActionResult.PASS;
    }
}
