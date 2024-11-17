package slimeknights.tconstruct.plugin;

import blusunrize.immersiveengineering.api.tool.ChemthrowerHandler;
import blusunrize.immersiveengineering.api.tool.ChemthrowerHandler.ChemthrowerEffect_Potion;
import blusunrize.immersiveengineering.api.tool.ChemthrowerHandler.ChemthrowerEffect_RandomTeleport;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import slimeknights.tconstruct.fluids.TinkerFluids;

/**
 * Event handlers to run when Immersive Engineering is present
 */
public class ImmersiveEngineeringPlugin {
    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        ChemthrowerHandler.registerFlammable(TinkerFluids.blazingBlood.getLocalTag());
        registerChemEffect(TinkerFluids.earthSlime.getForgeTag(), StatusEffects.SLOWNESS, 140);
        registerChemEffect(TinkerFluids.skySlime.getLocalTag(), StatusEffects.JUMP_BOOST, 200);
        registerChemEffect(TinkerFluids.enderSlime.getLocalTag(), StatusEffects.LEVITATION, 100);
        registerChemEffect(TinkerFluids.venom.getLocalTag(), StatusEffects.POISON, 300);
        registerChemEffect(TinkerFluids.magma.getForgeTag(), StatusEffects.FIRE_RESISTANCE, 200);
        registerChemEffect(TinkerFluids.liquidSoul.getForgeTag(), StatusEffects.BLINDNESS, 100);
        ChemthrowerHandler.registerEffect(TinkerFluids.moltenEnder.getForgeTag(), new ChemthrowerEffect_RandomTeleport(null, 0, 1));
        registerChemEffect(TinkerFluids.moltenUranium.getLocalTag(), StatusEffects.POISON, 200);
    }

    /**
     * Shorthand to register a chemical potion effect
     */
    private static void registerChemEffect(TagKey<Fluid> tag, StatusEffect effect, int duration) {
        ChemthrowerHandler.registerEffect(tag, new ChemthrowerEffect_Potion(null, 0, effect, duration, 0));
    }
}
