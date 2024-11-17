package slimeknights.tconstruct.tools.modifiers.ability.interaction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.Event.Result;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.events.TinkerToolEvent.ToolHarvestEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.BlockInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.modules.behavior.ShowOffhandModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.aoe.AreaOfEffectIterator;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class HarvestAbilityModifier extends NoLevelsModifier implements BlockInteractionModifierHook {
    private final int priority;

    @Override
    protected void registerHooks(Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addModule(ShowOffhandModule.DISALLOW_BROKEN);
        hookBuilder.addHook(this, ModifierHooks.BLOCK_INTERACT);
    }

    @Override
    public boolean shouldDisplay(boolean advanced) {
        return this.priority > Short.MIN_VALUE;
    }

    @Override
    public Text getDisplayName(IToolStackView tool, ModifierEntry entry) {
        return DualOptionInteraction.formatModifierName(tool, this, super.getDisplayName(tool, entry));
    }

    /**
     * Harvests a block that is harvested on interaction, such a berry bushes
     *
     * @param context Item use context of the original block clicked
     * @param world   Level instance
     * @param state   State to harvest
     * @param pos     Position to harvest
     * @param player  Player instance
     * @return True if harvested
     */

    private static boolean harvestInteract(ItemUsageContext context, ServerWorld world, BlockState state, BlockPos pos, @Nullable PlayerEntity player) {
        if (player == null) {
            return false;
        }
        BlockHitResult trace = new BlockHitResult(context.getHitPos(), context.getSide(), pos, false);
        ActionResult result = state.onUse(world, player, context.getHand(), trace);
        return result.isAccepted();
    }

    /**
     * Harvests a stackable block, like sugar cane or kelp
     *
     * @param world  Level instance
     * @param state  Block state
     * @param pos    Block position
     * @param player Player instance
     * @return True if the block was harvested
     */
    private static boolean harvestStackable(ServerWorld world, BlockState state, BlockPos pos, @Nullable PlayerEntity player) {
        // if the block below is the same, break this block
        if (world.getBlockState(pos.down()).getBlock() == state.getBlock()) {
            world.breakBlock(pos, true, player);
            return true;
        } else {
            // if the block above is the same, break it
            BlockPos up = pos.up();
            if (world.getBlockState(up).getBlock() == state.getBlock()) {
                world.breakBlock(up, true, player);
                return true;
            }
        }
        return false;
    }

    /**
     * Tries harvesting a normal crop, that is a crop that goes through a set number of stages and is broken to drop produce and seeds
     *
     * @param stack  Tool stack
     * @param world  Level instance
     * @param state  Block state
     * @param pos    Block position
     * @param player Player instance
     * @return True if the crop was successfully harvested
     */
    private static boolean harvestCrop(ItemStack stack, ServerWorld world, BlockState state, BlockPos pos, @Nullable PlayerEntity player) {
        Block block = state.getBlock();
        BlockState replant;
        // if crops block, its easy
        if (block instanceof CropBlock crops) {
            if (!crops.isMature(state)) {
                return false;
            }
            replant = crops.withAge(0);
        } else {
            // try to find an age property
            IntProperty age = null;
            for (Property<?> prop : state.getProperties()) {
                if (prop.getName().equals("age") && prop instanceof IntProperty) {
                    age = (IntProperty) prop;
                    break;
                }
            }
            // must have an age property
            if (age == null) {
                return false;
            } else {
                // property must have 0 as valid
                Collection<Integer> allowedValues = age.getValues();
                if (!allowedValues.contains(0)) {
                    return false;
                }
                // crop must be max age
                int maxAge = age.getValues().stream().max(Integer::compareTo).orElse(Integer.MAX_VALUE);
                if (state.get(age) < maxAge) {
                    return false;
                }
                replant = state.with(age, 0);
            }
        }

        // crop is fully grown, get loot context
        LootContextParameterSet.Builder lootContext = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                .add(LootContextParameters.TOOL, ItemStack.EMPTY)
                .addOptional(LootContextParameters.BLOCK_ENTITY, world.getBlockEntity(pos));
        // find drops
        List<ItemStack> drops = state.getDroppedStacks(lootContext);

        // find a seed to remove from the drops
        Iterator<ItemStack> iterator = drops.iterator();
        boolean hasSeed = false;
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            if (drop.isIn(TinkerTags.Items.SEEDS)) {
                hasSeed = true;
                drop.decrement(1);
                if (drop.isEmpty()) {
                    iterator.remove();
                }
                break;
            }
        }

        // if we found one, replant, no seed means break
        if (hasSeed) {
            world.setBlockState(pos, replant);
            state.onStacksDropped(world, pos, stack, true);
            // set block state will not play sounds, destory block will
            world.playSound(null, pos, state.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            world.breakBlock(pos, false);
        }

        // drop items
        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop);
        }

        return true;
    }

    /**
     * Tries to harvest the crop at the given position
     *
     * @param context Item use context of the original block clicked
     * @param world   Level instance
     * @param state   State to harvest
     * @param pos     Position to harvest
     * @param source  Source of the interaction
     * @return True if harvested
     */
    @SuppressWarnings("deprecation")
    private static boolean harvest(ItemUsageContext context, IToolStackView tool, ServerWorld world, BlockState state, BlockPos pos, InteractionSource source) {
        PlayerEntity player = context.getPlayer();
        // first, check main harvestable tag
        RegistryEntry<Block> holder = state.getBlock().getRegistryEntry();
        if (!holder.isIn(TinkerTags.Blocks.HARVESTABLE)) {
            return false;
        }
        // try harvest event
        boolean didHarvest = false;
        Result result = new ToolHarvestEvent(tool, context, world, state, pos, source).fire();
        if (result != Result.DEFAULT) {
            didHarvest = result == Result.ALLOW;

            // crops that work based on right click interact (berry bushes)
        } else if (holder.isIn(TinkerTags.Blocks.HARVESTABLE_INTERACT)) {
            didHarvest = harvestInteract(context, world, state, pos, player);

            // next, try sugar cane like blocks
        } else if (holder.isIn(TinkerTags.Blocks.HARVESTABLE_STACKABLE)) {
            didHarvest = harvestStackable(world, state, pos, player);

            // normal crops like wheat or carrots
        } else if (holder.isIn(TinkerTags.Blocks.HARVESTABLE_CROPS)) {
            didHarvest = harvestCrop(context.getStack(), world, state, pos, player);
        }

        // if we successfully harvested, run the modifier hook
        if (didHarvest) {
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.PLANT_HARVEST).afterHarvest(tool, entry, context, world, state, pos);
            }
        }

        return didHarvest;
    }

    @Override
    public ActionResult beforeBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        if (tool.isBroken() || !tool.getHook(ToolHooks.INTERACTION).canInteract(tool, modifier.getId(), source)) {
            return ActionResult.PASS;
        }

        // skip if sneaking
        PlayerEntity player = context.getPlayer();
        if (player != null && player.isSneaking()) {
            return ActionResult.PASS;
        }

        // try harvest first
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.isIn(TinkerTags.Blocks.HARVESTABLE)) {
            if (world instanceof ServerWorld server) {
                boolean survival = player == null || !player.isCreative();

                // try harvesting the crop, if successful and survival, damage the tool
                boolean didHarvest = false;
                boolean broken = false;
                ItemStack stack = context.getStack();
                if (harvest(context, tool, server, state, pos, source)) {
                    didHarvest = true;
                    broken = survival && ToolDamageUtil.damage(tool, 1, player, stack);
                }

                // if we have a player and harvest logic, try doing AOE harvest
                if (!broken && player != null) {
                    for (BlockPos newPos : tool.getHook(ToolHooks.AOE_ITERATOR).getBlocks(tool, stack, player, state, world, pos, context.getSide(), AreaOfEffectIterator.AOEMatchType.TRANSFORM)) {
                        // try harvesting the crop, if successful and survival, damage the tool
                        if (harvest(context, tool, server, world.getBlockState(newPos), newPos, source)) {
                            didHarvest = true;
                            if (survival && ToolDamageUtil.damage(tool, 1, player, stack)) {
                                broken = true;
                                break;
                            }
                        }
                    }
                }
                // animations
                if (player != null) {
                    if (didHarvest) {
                        player.spawnSweepAttackParticles();
                    }
                    if (broken) {
                        player.sendToolBreakStatus(context.getHand());
                    }
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
