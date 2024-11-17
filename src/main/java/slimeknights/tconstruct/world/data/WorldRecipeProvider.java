package slimeknights.tconstruct.world.data;

import net.minecraft.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.Tags;
import slimeknights.mantle.recipe.data.ICommonRecipeHelper;
import slimeknights.tconstruct.common.data.BaseRecipeProvider;
import slimeknights.tconstruct.common.json.ConfigEnabledCondition;
import slimeknights.tconstruct.common.registration.GeodeItemObject;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.function.Consumer;

public class WorldRecipeProvider extends BaseRecipeProvider implements ICommonRecipeHelper {
    public WorldRecipeProvider(DataGenerator generator) {
        super(generator);
    }

    @Override
    public String getName() {
        return "Tinkers' Construct World Recipes";
    }

    @Override
    protected void buildCraftingRecipes(Consumer<RecipeJsonProvider> consumer) {
        // Add recipe for all slimeball <-> congealed and slimeblock <-> slimeball
        // only earth slime recipe we need here slime
        ShapedRecipeJsonBuilder.create(TinkerWorld.congealedSlime.get(SlimeType.EARTH))
                .define('#', SlimeType.EARTH.getSlimeballTag())
                .pattern("##")
                .pattern("##")
                .unlockedBy("has_item", conditionsFromItem(SlimeType.EARTH.getSlimeballTag()))
                .group("tconstruct:congealed_slime")
                .save(consumer, this.location("common/slime/earth/congealed"));

        // does not need green as its the fallback
        for (SlimeType slimeType : SlimeType.TINKER) {
            Identifier name = this.location("common/slime/" + slimeType.asString() + "/congealed");
            ShapedRecipeJsonBuilder.create(TinkerWorld.congealedSlime.get(slimeType))
                    .define('#', slimeType.getSlimeballTag())
                    .pattern("##")
                    .pattern("##")
                    .unlockedBy("has_item", conditionsFromItem(slimeType.getSlimeballTag()))
                    .group("tconstruct:congealed_slime")
                    .save(consumer, name);
            Identifier blockName = this.location("common/slime/" + slimeType.asString() + "/slimeblock");
            ShapedRecipeJsonBuilder.create(TinkerWorld.slime.get(slimeType))
                    .define('#', slimeType.getSlimeballTag())
                    .pattern("###")
                    .pattern("###")
                    .pattern("###")
                    .unlockedBy("has_item", conditionsFromItem(slimeType.getSlimeballTag()))
                    .group("slime_blocks")
                    .save(consumer, blockName);
            // green already can craft into slime balls
            ShapelessRecipeJsonBuilder.create(TinkerCommons.slimeball.get(slimeType), 9)
                    .requires(TinkerWorld.slime.get(slimeType))
                    .unlockedBy("has_item", conditionsFromItem(TinkerWorld.slime.get(slimeType)))
                    .group("tconstruct:slime_balls")
                    .save(consumer, "tconstruct:common/slime/" + slimeType.asString() + "/slimeball_from_block");
        }
        // all types of congealed need a recipe to a block
        for (SlimeType slimeType : SlimeType.values()) {
            ShapelessRecipeJsonBuilder.create(TinkerCommons.slimeball.get(slimeType), 4)
                    .requires(TinkerWorld.congealedSlime.get(slimeType))
                    .unlockedBy("has_item", conditionsFromItem(TinkerWorld.congealedSlime.get(slimeType)))
                    .group("tconstruct:slime_balls")
                    .save(consumer, "tconstruct:common/slime/" + slimeType.asString() + "/slimeball_from_congealed");
        }

        // craft other slime based items, forge does not automatically add recipes using the tag anymore
        Consumer<RecipeJsonProvider> slimeConsumer = withCondition(consumer, ConfigEnabledCondition.SLIME_RECIPE_FIX);
        ShapedRecipeJsonBuilder.create(Blocks.STICKY_PISTON)
                .pattern("#")
                .pattern("P")
                .define('#', Tags.Items.SLIMEBALLS)
                .define('P', Blocks.PISTON)
                .unlockedBy("has_slime_ball", conditionsFromItem(Tags.Items.SLIMEBALLS))
                .save(slimeConsumer, this.location("common/slime/sticky_piston"));
        ShapedRecipeJsonBuilder.create(Items.LEAD, 2)
                .define('~', Items.STRING)
                .define('O', Tags.Items.SLIMEBALLS)
                .pattern("~~ ")
                .pattern("~O ")
                .pattern("  ~")
                .unlockedBy("has_slime_ball", conditionsFromItem(Tags.Items.SLIMEBALLS))
                .save(slimeConsumer, this.location("common/slime/lead"));
        ShapelessRecipeJsonBuilder.create(Items.MAGMA_CREAM)
                .requires(Items.BLAZE_POWDER)
                .requires(Tags.Items.SLIMEBALLS)
                .unlockedBy("has_blaze_powder", conditionsFromItem(Items.BLAZE_POWDER))
                .save(slimeConsumer, this.location("common/slime/magma_cream"));

        // wood
        String woodFolder = "world/wood/";
        this.woodCrafting(consumer, TinkerWorld.greenheart, woodFolder + "greenheart/");
        this.woodCrafting(consumer, TinkerWorld.skyroot, woodFolder + "skyroot/");
        this.woodCrafting(consumer, TinkerWorld.bloodshroom, woodFolder + "bloodshroom/");
        this.woodCrafting(consumer, TinkerWorld.enderbark, woodFolder + "enderbark/");

        // geodes
        this.geodeRecipes(consumer, TinkerWorld.earthGeode, SlimeType.EARTH, "common/slime/earth/");
        this.geodeRecipes(consumer, TinkerWorld.skyGeode, SlimeType.SKY, "common/slime/sky/");
        this.geodeRecipes(consumer, TinkerWorld.ichorGeode, SlimeType.ICHOR, "common/slime/ichor/");
        this.geodeRecipes(consumer, TinkerWorld.enderGeode, SlimeType.ENDER, "common/slime/ender/");
    }

    private void geodeRecipes(Consumer<RecipeJsonProvider> consumer, GeodeItemObject geode, SlimeType slime, String folder) {
        ShapedRecipeJsonBuilder.create(geode.getBlock())
                .define('#', geode.asItem())
                .pattern("##")
                .pattern("##")
                .unlockedBy("has_item", conditionsFromItem(geode.asItem()))
                .group("tconstruct:slime_crystal_block")
                .save(consumer, this.location(folder + "crystal_block"));
        CookingRecipeJsonBuilder.createBlasting(Ingredient.ofItems(geode), TinkerCommons.slimeball.get(slime), 0.2f, 200)
                .unlockedBy("has_crystal", conditionsFromItem(geode))
                .group("tconstruct:slime_crystal")
                .save(consumer, this.location(folder + "crystal_smelting"));
        ItemConvertible dirt = TinkerWorld.slimeDirt.get(slime.asDirt());
        CookingRecipeJsonBuilder.createBlasting(Ingredient.ofItems(dirt), geode, 0.2f, 400)
                .unlockedBy("has_dirt", conditionsFromItem(dirt))
                .group("tconstruct:slime_dirt")
                .save(consumer, this.location(folder + "crystal_growing"));
    }
}
