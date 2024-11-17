package slimeknights.mantle.fluid.transfer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataOutput.OutputType;
import net.minecraft.data.DataWriter;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Data gen for fluid transfer logic
 */
@SuppressWarnings("unused")
public abstract class AbstractFluidContainerTransferProvider extends GenericDataProvider {
    private final Map<Identifier, TransferJson> allTransfers = new HashMap<>();
    private final String modId;

    public AbstractFluidContainerTransferProvider(DataGenerator generator, String modId) {
        super(generator, OutputType.DATA_PACK, FluidContainerTransferManager.FOLDER, FluidContainerTransferManager.GSON);
        this.modId = modId;
    }

    /**
     * Function to add all relevant transfers
     */
    protected abstract void addTransfers();

    /**
     * Adds a transfer to be saved
     */
    protected void addTransfer(Identifier id, IFluidContainerTransfer transfer, ICondition... conditions) {
        TransferJson previous = this.allTransfers.putIfAbsent(id, new TransferJson(transfer, conditions));
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate fluid container transfer " + id);
        }
    }

    /**
     * Adds a transfer to be saved
     */
    protected void addTransfer(String name, IFluidContainerTransfer transfer, ICondition... conditions) {
        this.addTransfer(new Identifier(this.modId, name), transfer, conditions);
    }

    /**
     * Adds generic fill and empty for a container
     */
    protected void addFillEmpty(String prefix, ItemConvertible item, ItemConvertible container, Fluid fluid, TagKey<Fluid> tag, int amount, ICondition... conditions) {
        this.addTransfer(prefix + "empty", new EmptyFluidContainerTransfer(Ingredient.ofItems(item), ItemOutput.fromItem(container), new FluidStack(fluid, amount)), conditions);
        this.addTransfer(prefix + "fill", new FillFluidContainerTransfer(Ingredient.ofItems(container), ItemOutput.fromItem(item), FluidIngredient.of(tag, amount)), conditions);
    }

    /**
     * Adds generic fill and empty for a container
     */
    protected void addFillEmptyNBT(String prefix, ItemConvertible item, ItemConvertible container, Fluid fluid, TagKey<Fluid> tag, int amount, ICondition... conditions) {
        this.addTransfer(prefix + "empty", new EmptyFluidWithNBTTransfer(Ingredient.ofItems(item), ItemOutput.fromItem(container), new FluidStack(fluid, amount)), conditions);
        this.addTransfer(prefix + "fill", new FillFluidWithNBTTransfer(Ingredient.ofItems(container), ItemOutput.fromItem(item), FluidIngredient.of(tag, amount)), conditions);
    }

    @Override
    public CompletableFuture<?> run(DataWriter cache) {
        this.addTransfers();
        return this.allOf(this.allTransfers.entrySet().stream().map(entry -> this.saveJson(cache, entry.getKey(), entry.getValue().toJson())));
    }

    /**
     * Json with transfer and condition
     */
    private record TransferJson(IFluidContainerTransfer transfer, ICondition[] conditions) {
        /**
         * Serializes this to JSON
         */
        public JsonElement toJson() {
            JsonElement element = FluidContainerTransferManager.GSON.toJsonTree(this.transfer, IFluidContainerTransfer.class);
            assert element.isJsonObject();
            if (this.conditions.length != 0) {
                JsonArray array = new JsonArray();
                for (ICondition condition : this.conditions) {
                    array.add(CraftingHelper.serialize(condition));
                }
                element.getAsJsonObject().add("conditions", array);
            }
            return element;
        }
    }
}
