package slimeknights.tconstruct.library.client.book.content;

import com.google.common.collect.ImmutableList;
import slimeknights.mantle.client.screen.book.element.ItemElement;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.book.elements.TinkerItemElement;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.item.ArmorSlotType;
import slimeknights.tconstruct.tools.stats.SkullStats;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

/**
 * Extension of the material page to display skull stats for the slimeskull
 */
public class ContentMaterialSkull extends AbstractMaterialContent {
    /**
     * Translation key for skull recipe
     */
    private static final String SKULL_FROM = TConstruct.makeTranslationKey("book", "material.skull_from");
    /**
     * Page ID for using this index directly
     */
    public static final Identifier ID = TConstruct.getResource("skull_material");

    /**
     * casting recipe used to create this item
     */
    protected transient IDisplayableCastingRecipe skullRecipe = null;
    /**
     * If true, casting recipe was looked up
     */
    private transient boolean searchedSkullRecipe = false;
    /**
     * List of skull items as inputs
     */
    protected transient List<ItemStack> skullStacks = null;

    public ContentMaterialSkull(MaterialVariantId material, boolean detailed) {
        super(material, detailed);
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Nullable
    @Override
    protected MaterialStatsId getStatType(int index) {
        return index == 0 ? SkullStats.ID : null;
    }

    @Override
    protected String getTextKey(MaterialId material) {
        return String.format(this.detailed ? "material.%s.%s.skull_encyclopedia" : "material.%s.%s.skull_flavor", material.getNamespace(), material.getPath());
    }

    /**
     * Gets the recipe to cast this skull
     */
    @Nullable
    private IDisplayableCastingRecipe getSkullRecipe() {
        World world = MinecraftClient.getInstance().world;
        if (!this.searchedSkullRecipe && world != null) {
            this.skullRecipe = world.getRecipeManager().listAllOfType(TinkerRecipeTypes.CASTING_BASIN.get()).stream()
                    .filter(recipe -> recipe instanceof IDisplayableCastingRecipe)
                    .map(recipe -> (IDisplayableCastingRecipe) recipe)
                    .filter(recipe -> {
                        ItemStack output = recipe.getOutput();
                        return output.getItem() == TinkerTools.slimesuit.get(ArmorSlotType.HELMET) && MaterialIdNBT.from(output).getMaterial(0).getId().toString().equals(this.materialName);
                    })
                    .findFirst()
                    .orElse(null);
            this.searchedSkullRecipe = true;
        }
        return this.skullRecipe;
    }

    @Override
    public Text getTitleComponent() {
        // display slimeskull instead of material name
        IDisplayableCastingRecipe skullRecipe = this.getSkullRecipe();
        if (skullRecipe != null) {
            return skullRecipe.getOutput().getName();
        }
        return super.getTitleComponent();
    }

    @Override
    public List<ItemStack> getDisplayStacks() {
        // display skull items instead of repair items
        IDisplayableCastingRecipe skullRecipe = this.getSkullRecipe();
        if (skullRecipe != null) {
            List<ItemStack> skulls = skullRecipe.getCastItems();
            if (!skulls.isEmpty()) {
                return skulls;
            }
        }
        return super.getDisplayStacks();
    }

    @Override
    protected boolean supportsStatType(MaterialStatsId statsId) {
        return statsId.equals(SkullStats.ID); // support only skulls
    }

    @Override
    protected void addPrimaryDisplayItems(List<ItemElement> displayTools, MaterialVariantId materialId) {
        displayTools.add(new TinkerItemElement(TinkerToolParts.repairKit.get().withMaterialForDisplay(materialId)));

        super.addPrimaryDisplayItems(displayTools, materialId);

        // add skull recipe to display items
        IDisplayableCastingRecipe skullRecipe = this.getSkullRecipe();
        if (skullRecipe != null) {
            // add repair kit
            List<ItemStack> casts = skullRecipe.getCastItems();
            if (!casts.isEmpty()) {
                ItemElement elementItem = new TinkerItemElement(0, 0, 1, casts);
                elementItem.tooltip = ImmutableList.of(Text.translatable(SKULL_FROM, casts.get(0).getName()));
                displayTools.add(elementItem);
            }
        }
    }
}
