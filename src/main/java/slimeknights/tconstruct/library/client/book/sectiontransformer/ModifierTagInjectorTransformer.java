package slimeknights.tconstruct.library.client.book.sectiontransformer;

import slimeknights.mantle.client.book.data.content.PageContent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.book.content.ContentModifier;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierManager;

import java.util.Iterator;

import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Injects modifiers into a section based on a tag
 */
public class ModifierTagInjectorTransformer extends AbstractTagInjectingTransformer<Modifier> {
    public static final ModifierTagInjectorTransformer INSTANCE = new ModifierTagInjectorTransformer();

    private ModifierTagInjectorTransformer() {
        super(ModifierManager.REGISTRY_KEY, TConstruct.getResource("load_modifiers"), ContentModifier.ID);
    }

    @Override
    protected Iterator<Modifier> getTagEntries(TagKey<Modifier> tag) {
        return ModifierManager.getTagValues(tag).iterator();
    }

    @Override
    protected Identifier getId(Modifier modifier) {
        return modifier.getId();
    }

    @Override
    protected PageContent createFallback(Modifier modifier) {
        return new ContentModifier(modifier);
    }
}
