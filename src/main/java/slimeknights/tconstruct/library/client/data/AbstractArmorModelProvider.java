package slimeknights.tconstruct.library.client.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataWriter;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModel;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Data provider for armor models
 */
public abstract class AbstractArmorModelProvider extends GenericDataProvider {
    private final Map<Identifier, ArmorModel> models = new HashMap<>();

    public AbstractArmorModelProvider(DataGenerator generator) {
        super(generator, ResourceType.CLIENT_RESOURCES, ArmorModelManager.FOLDER);
    }

    /**
     * Add all models to the manager
     */
    protected abstract void addModels();

    @Override
    public void run(DataWriter output) throws IOException {
        addModels();
        models.forEach((id, model) -> saveJson(output, id, ArmorModel.LOADABLE.serialize(model)));
    }

    /**
     * Adds a model to the generator
     */
    protected void addModel(Identifier name, ArmorTextureSupplier... layers) {
        ArmorModel existing = this.models.putIfAbsent(name, new ArmorModel(List.of(layers)));
        if (existing != null) {
            throw new IllegalArgumentException("Duplicate armor model at " + name + ", previous value " + existing);
        }
    }

    /**
     * Adds a model to the generator
     */
    protected void addModel(IdAwareObject name, ArmorTextureSupplier... layers) {
        addModel(name.getId(), layers);
    }

    /**
     * Adds a model to the generator
     */
    protected void addModel(IdAwareObject name, Function<Identifier, ArmorTextureSupplier[]> layers) {
        addModel(name.getId(), layers.apply(name.getId()));
    }
}
