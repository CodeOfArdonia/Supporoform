package slimeknights.mantle.client.book.structure.level;

import net.minecraft.entity.Entity;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.entity.EntityLookup;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Implementation of an entity getter for a world with no entities
 */
public class FakeEntityGetter implements EntityLookup<Entity> {
    public static final FakeEntityGetter INSTANCE = new FakeEntityGetter();

    private FakeEntityGetter() {
    }

    @Nullable
    @Override
    public Entity get(int id) {
        return null;
    }

    @Nullable
    @Override
    public Entity get(UUID pUuid) {
        return null;
    }

    @Override
    public Iterable<Entity> iterate() {
        return Collections.emptyList();
    }

    @Override
    public <U extends Entity> void forEach(TypeFilter<Entity, U> typeTest, LazyIterationConsumer<U> successConsumer) {
    }

    @Override
    public void forEachIntersects(Box aabb, Consumer<Entity> successConsumer) {
    }

    @Override
    public <U extends Entity> void forEachIntersects(TypeFilter<Entity, U> typeTest, Box bounds, LazyIterationConsumer<U> successConsumer) {
    }
}
