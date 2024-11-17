package slimeknights.tconstruct.library.modifiers.modules.behavior;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Module which performs AOE removing of campfires
 */
public record ExtinguishCampfireModule(
        ModifierCondition<IToolStackView> condition) implements BlockTransformModule, ConditionalModule<IToolStackView> {
    public static final ExtinguishCampfireModule INSTANCE = new ExtinguishCampfireModule(ModifierCondition.ANY_TOOL);
    public static final RecordLoadable<ExtinguishCampfireModule> LOADER = RecordLoadable.create(ModifierCondition.TOOL_FIELD, ExtinguishCampfireModule::new);

    @Override
    public IGenericLoader<? extends ModifierModule> getLoader() {
        return LOADER;
    }

    @Override
    public boolean requireGround() {
        return false;
    }

    @Override
    public ActionResult afterBlockUse(IToolStackView tool, ModifierEntry modifier, ItemUsageContext context, InteractionSource source) {
        if (this.condition.matches(tool, modifier)) {
            return BlockTransformModule.super.afterBlockUse(tool, modifier, context, source);
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean transform(IToolStackView tool, ItemUsageContext context, BlockState original, boolean playSound) {
        if (original.getBlock() instanceof CampfireBlock && original.get(CampfireBlock.LIT)) {
            World level = context.getWorld();
            BlockPos pos = context.getBlockPos();
            if (!level.isClient) {
                if (playSound) {
                    level.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                CampfireBlock.extinguish(context.getPlayer(), level, pos, original);
            }
            level.setBlockState(pos, original.with(CampfireBlock.LIT, false), Block.field_31022);
            return true;
        }
        return false;
    }
}
