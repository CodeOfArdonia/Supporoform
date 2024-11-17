package slimeknights.tconstruct.world;

import net.minecraft.block.SkullBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import slimeknights.mantle.loot.function.SetFluidLootFunction;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.json.loot.AddToolDataFunction;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.Arrays;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldEvents {
    /* Loot injection */

    /**
     * Injects an entry into a loot pool
     *
     * @param event    Loot table event
     * @param poolName Pool name
     * @param entries  Entry to inject
     */
    private static void injectInto(LootTableLoadEvent event, String poolName, LootPoolEntry... entries) {
        LootPool pool = event.getTable().getPool(poolName);
        //noinspection ConstantConditions method is annotated wrongly
        if (pool != null) {
            int oldLength = pool.entries.length;
            pool.entries = Arrays.copyOf(pool.entries, oldLength + entries.length);
            System.arraycopy(entries, 0, pool.entries, oldLength, entries.length);
        }
    }

    /**
     * Makes a seed injection loot entry
     */
    private static LootPoolEntry makeSeed(FoliageType type, int weight) {
        return ItemEntry.builder(TinkerWorld.slimeGrassSeeds.get(type)).weight(weight)
                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 4))).build();
    }

    /**
     * Makes a sapling injection loot entry
     */
    private static LootPoolEntry makeSapling(FoliageType type, int weight) {
        return ItemEntry.builder(TinkerWorld.slimeSapling.get(type)).weight(weight).build();
    }

    @SubscribeEvent
    static void onLootTableLoad(LootTableLoadEvent event) {
        Identifier name = event.getName();
        if ("minecraft".equals(name.getNamespace())) {
            switch (name.getPath()) {
                // sky
                case "chests/simple_dungeon":
                    if (Config.COMMON.slimyLootChests.get()) {
                        injectInto(event, "pool1", makeSeed(FoliageType.EARTH, 3), makeSeed(FoliageType.SKY, 7));
                        injectInto(event, "main", makeSapling(FoliageType.EARTH, 3), makeSapling(FoliageType.SKY, 7));
                    }
                    break;
                // ichor
                case "chests/nether_bridge":
                    if (Config.COMMON.slimyLootChests.get()) {
                        injectInto(event, "main", makeSeed(FoliageType.BLOOD, 5));
                    }
                    break;
                case "chests/bastion_bridge":
                    if (Config.COMMON.slimyLootChests.get()) {
                        injectInto(event, "pool2", makeSapling(FoliageType.BLOOD, 1));
                    }
                    break;
                // ender
                case "chests/end_city_treasure":
                    if (Config.COMMON.slimyLootChests.get()) {
                        injectInto(event, "main", makeSeed(FoliageType.ENDER, 5), makeSapling(FoliageType.ENDER, 3));
                    }
                    break;

                // barter for molten blaze lanterns
                case "gameplay/piglin_bartering": {
                    int weight = Config.COMMON.barterBlazingBlood.get();
                    if (weight > 0) {
                        injectInto(event, "main", ItemEntry.builder(TinkerSmeltery.scorchedLantern).weight(weight)
                                .apply(SetFluidLootFunction.builder(new FluidStack(TinkerFluids.blazingBlood.get(), FluidValues.LANTERN_CAPACITY)))
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 4)))
                                .build());
                    }
                    break;
                }

                // randomly swap vanilla tool for a tinkers tool
                case "chests/spawn_bonus_chest": {
                    int weight = Config.COMMON.tinkerToolBonusChest.get();
                    if (weight > 0) {
                        RandomMaterial randomTier1 = RandomMaterial.random().tier(1).build();
                        RandomMaterial firstWithStat = RandomMaterial.firstWithStat(); // should be wood
                        injectInto(event, "main", ItemEntry.builder(TinkerTools.handAxe.get())
                                .weight(weight)
                                .apply(AddToolDataFunction.builder()
                                        .addMaterial(randomTier1)
                                        .addMaterial(firstWithStat)
                                        .addMaterial(randomTier1))
                                .build());
                        injectInto(event, "pool1", ItemEntry.builder(TinkerTools.pickaxe.get())
                                .weight(weight)
                                .apply(AddToolDataFunction.builder()
                                        .addMaterial(randomTier1)
                                        .addMaterial(firstWithStat)
                                        .addMaterial(randomTier1))
                                .build());
                    }
                    break;
                }
            }
        }
    }


    /* Heads */

    @SubscribeEvent
    static void livingVisibility(LivingVisibilityEvent event) {
        Entity lookingEntity = event.getLookingEntity();
        if (lookingEntity == null) {
            return;
        }
        LivingEntity entity = event.getEntity();
        ItemStack helmet = entity.getEquippedStack(EquipmentSlot.HEAD);
        Item item = helmet.getItem();
        if (item != Items.AIR && TinkerWorld.headItems.contains(item)) {
            if (lookingEntity.getType() == ((TinkerHeadType) ((SkullBlock) ((BlockItem) item).getBlock()).getSkullType()).getType()) {
                event.modifyVisibility(0.5f);
            }
        }
    }

    @SubscribeEvent
    static void creeperKill(LivingDropsEvent event) {
        DamageSource source = event.getSource();
        if (source != null) {
            Entity entity = source.getAttacker();
            if (entity instanceof CreeperEntity creeper) {
                if (creeper.shouldDropHead()) {
                    LivingEntity dying = event.getEntity();
                    TinkerHeadType headType = TinkerHeadType.fromEntityType(dying.getType());
                    if (headType != null && Config.COMMON.headDrops.get(headType).get()) {
                        creeper.onHeadDropped();
                        event.getDrops().add(dying.dropItem(TinkerWorld.heads.get(headType)));
                    }
                }
            }
        }
    }
}
