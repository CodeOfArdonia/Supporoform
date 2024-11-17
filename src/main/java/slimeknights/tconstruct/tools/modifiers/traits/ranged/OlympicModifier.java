package slimeknights.tconstruct.tools.modifiers.traits.ranged;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.recipe.helper.TagPreference;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.shared.TinkerMaterials;

public class OlympicModifier extends Modifier implements ProjectileLaunchModifierHook, ProjectileHitModifierHook {
    private static final Identifier OLYMPIC_START = TConstruct.getResource("olympic_start");
    private static final TagKey<Item> PLATINUM_NUGGET = ItemTags.create(new Identifier("forge", "nuggets/platinum"));

    /**
     * Gets the nugget for the given distance
     */
    private static Item getNugget(double distanceSq) {
        // 50 meters - platinum
        if (distanceSq > 2500) {
            return TagPreference.getPreference(PLATINUM_NUGGET).orElse(TinkerMaterials.cobalt.getNugget());
        }
        // 40 meters - gold
        if (distanceSq > 1600) {
            return Items.GOLD_NUGGET;
        }
        // 30 meters - iron
        if (distanceSq > 900) {
            return Items.IRON_NUGGET;
        }
        // 20 meters - copper
        if (distanceSq > 400) {
            return TinkerMaterials.copperNugget.get();
        }
        return Items.AIR;
    }

    @Override
    protected void registerHooks(Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.PROJECTILE_LAUNCH, ModifierHooks.PROJECTILE_HIT);
    }

    @Override
    public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, ProjectileEntity projectile, @Nullable PersistentProjectileEntity arrow, NamespacedNBT persistentData, boolean primary) {
        // store fired position
        NbtCompound tag = new NbtCompound();
        tag.putDouble("X", shooter.getX());
        tag.putDouble("Y", shooter.getY());
        tag.putDouble("Z", shooter.getZ());
        persistentData.put(OLYMPIC_START, tag);
    }

    @Override
    public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, ProjectileEntity projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
        // 10% chance per level
        Entity targetEntity = hit.getEntity();
        if (!projectile.getWorld().isClient && targetEntity.getType().getSpawnGroup() == SpawnGroup.MONSTER && RANDOM.nextInt(20) < modifier.getLevel()) {
            NbtCompound startCompound = persistentData.getCompound(OLYMPIC_START);
            if (!startCompound.isEmpty() && startCompound.contains("X", NbtElement.NUMBER_TYPE) && startCompound.contains("Y", NbtElement.NUMBER_TYPE) && startCompound.contains("Z", NbtElement.NUMBER_TYPE)) {
                // nugget type based on distance
                Item nugget = getNugget(targetEntity.squaredDistanceTo(startCompound.getDouble("X"), startCompound.getDouble("Y"), startCompound.getDouble("Z")));
                if (nugget != Items.AIR) {
                    // spawn and play sound
                    targetEntity.dropItem(nugget);
                    if (attacker != null) {
                        projectile.world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    }
                }
            }
        }
        return false;
    }
}
