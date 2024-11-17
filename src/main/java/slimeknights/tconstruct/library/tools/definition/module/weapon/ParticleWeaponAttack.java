package slimeknights.tconstruct.library.tools.definition.module.weapon;

import net.minecraft.registry.Registries;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

import net.minecraft.particle.DefaultParticleType;

/**
 * Weapon attack that just spawns an extra particle
 */
public record ParticleWeaponAttack(DefaultParticleType particle) implements MeleeHitToolHook, ToolModule {
    public static final RecordLoadable<ParticleWeaponAttack> LOADER = RecordLoadable.create(
            Loadables.PARTICLE_TYPE.comapFlatMap((type, error) -> {
                if (type instanceof DefaultParticleType simple) {
                    return simple;
                }
                throw error.create("Expected particle " + Registries.PARTICLE_TYPE.getKey(type) + " be a simple particle, got " + type);
            }, type -> type).requiredField("particle", ParticleWeaponAttack::particle), ParticleWeaponAttack::new);
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ParticleWeaponAttack>defaultHooks(ToolHooks.MELEE_HIT);

    @Override
    public RecordLoadable<ParticleWeaponAttack> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ToolAttackContext context, float damage) {
        if (context.isFullyCharged()) {
            ToolAttackUtil.spawnAttackParticle(this.particle, context.getAttacker(), 0.8d);
        }
    }
}
