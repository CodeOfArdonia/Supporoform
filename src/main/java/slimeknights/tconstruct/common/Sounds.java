package slimeknights.tconstruct.common;

import lombok.Getter;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * All sounds registered by Tinkers, should be used instead of vanilla events when subtitles need to be distinguished
 */
@Mod.EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public enum Sounds {
    // blocks
    SAW("little_saw"),
    ITEM_FRAME_CLICK,
    CASTING_COOLS,
    CASTING_CLICKS,

    // earth crystals
    EARTH_CRYSTAL_CHIME("block.earth_crystal.chime"),
    SKY_CRYSTAL_CHIME("block.sky_crystal.chime"),
    ICHOR_CRYSTAL_CHIME("block.ichor_crystal.chime"),
    ENDER_CRYSTAL_CHIME("block.ender_crystal.chime"),

    // tools
    SLIME_SLING,
    SLIME_SLING_TELEPORT("slime_sling.teleport"),
    THROWBALL_THROW("throw.throwball"),
    SHURIKEN_THROW("throw.shuriken"),
    LONGBOW_CHARGE("longbow.charge"),
    CRYSTALSHOT,
    BONK,

    // modifiers
    NECROTIC_HEAL,
    ENDERPORTING,
    EXTRA_JUMP,

    // entity
    SLIME_TELEPORT,
    SLIMY_BOUNCE("slimy_bounce"),

    // equip sounds
    EQUIP_SLIME("equip.slime"),
    EQUIP_TRAVELERS("equip.travelers"),
    EQUIP_PLATE("equip.plate"),

    // unused
    TOY_SQUEAK,
    CROSSBOW_RELOAD,
    STONE_HIT,
    WOOD_HIT,
    CHARGED,
    DISCHARGE;

    @Getter
    private final SoundEvent sound;

    public static final BlockSoundGroup EARTH_CRYSTAL = makeCrystalSound(0.75f);
    public static final Map<BudSize, BlockSoundGroup> EARTH_CRYSTAL_CLUSTER = makeClusterSounds(0.75f);
    public static final BlockSoundGroup SKY_CRYSTAL = makeCrystalSound(1.2f);
    public static final Map<BudSize, BlockSoundGroup> SKY_CRYSTAL_CLUSTER = makeClusterSounds(1.2f);
    public static final BlockSoundGroup ICHOR_CRYSTAL = makeCrystalSound(0.35f);
    public static final Map<BudSize, BlockSoundGroup> ICHOR_CRYSTAL_CLUSTER = makeClusterSounds(0.35f);
    public static final BlockSoundGroup ENDER_CRYSTAL = makeCrystalSound(1.45f);
    public static final Map<BudSize, BlockSoundGroup> ENDER_CRYSTAL_CLUSTER = makeClusterSounds(1.45f);

    Sounds(String name) {
        Identifier registryName = TConstruct.getResource(name);
        sound = new SoundEvent(registryName);
    }

    Sounds() {
        String name = name().toLowerCase(Locale.US);
        Identifier registryName = TConstruct.getResource(name);
        this.sound = new SoundEvent(registryName);
    }

    @SubscribeEvent
    public static void registerSounds(RegisterEvent event) {
        if (event.getRegistryKey() == Registry.SOUND_EVENT_REGISTRY) {
            for (Sounds sound : values()) {
                ForgeRegistries.SOUND_EVENTS.register(sound.sound.getId(), sound.getSound());
            }
        }
    }

    /**
     * Makes sound type for crystals
     */
    private static BlockSoundGroup makeCrystalSound(float pitch) {
        return new BlockSoundGroup(1.0f, pitch, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundEvents.BLOCK_AMETHYST_BLOCK_FALL);
    }

    /**
     * Makes sound type for clusters
     */
    private static Map<BudSize, BlockSoundGroup> makeClusterSounds(float pitch) {
        Map<BudSize, BlockSoundGroup> map = new EnumMap<>(BudSize.class);
        map.put(BudSize.CLUSTER, new BlockSoundGroup(1.0f, pitch, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundEvents.BLOCK_AMETHYST_CLUSTER_FALL));
        map.put(BudSize.SMALL, new BlockSoundGroup(1.0f, pitch, SoundEvents.BLOCK_SMALL_AMETHYST_BUD_BREAK, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundEvents.BLOCK_SMALL_AMETHYST_BUD_PLACE, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundEvents.BLOCK_AMETHYST_CLUSTER_FALL));
        map.put(BudSize.MEDIUM, new BlockSoundGroup(1.0f, pitch, SoundEvents.BLOCK_MEDIUM_AMETHYST_BUD_BREAK, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundEvents.BLOCK_MEDIUM_AMETHYST_BUD_PLACE, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundEvents.BLOCK_AMETHYST_CLUSTER_FALL));
        map.put(BudSize.LARGE, new BlockSoundGroup(1.0f, pitch, SoundEvents.BLOCK_LARGE_AMETHYST_BUD_BREAK, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundEvents.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundEvents.BLOCK_AMETHYST_CLUSTER_FALL));
        return map;
    }
}
