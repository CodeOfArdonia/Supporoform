package slimeknights.tconstruct.library.client.book.sectiontransformer;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import slimeknights.mantle.client.book.data.content.PageContent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.book.content.ContentTool;

/**
 * Injects tools into a section based on a tag
 */
public class ToolTagInjectorTransformer extends AbstractTagInjectingTransformer<Item> {
    public static final ToolTagInjectorTransformer INSTANCE = new ToolTagInjectorTransformer();

    private ToolTagInjectorTransformer() {
        super(RegistryKeys.ITEM, TConstruct.getResource("load_tools"), ContentTool.ID);
    }

    @Override
    protected Identifier getId(Item item) {
        return Registries.ITEM.getId(item);
    }

    @Override
    protected PageContent createFallback(Item item) {
        return new ContentTool(item);
    }
}
