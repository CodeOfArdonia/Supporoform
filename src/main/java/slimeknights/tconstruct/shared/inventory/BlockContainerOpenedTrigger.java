package slimeknights.tconstruct.shared.inventory;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.advancement.criterion.EntityPredicate.Composite;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;

import java.util.Objects;

/**
 * Criteria that triggers when a container is opened
 */
public class BlockContainerOpenedTrigger extends AbstractCriterion<BlockContainerOpenedTrigger.Instance> {
    private static final Identifier ID = TConstruct.getResource("block_container_opened");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, Composite entityPredicate, AdvancementEntityPredicateDeserializer conditionsParser) {
        Identifier id = new Identifier(JsonHelper.getString(json, "type"));
        BlockEntityType<?> type = Registries.BLOCK_ENTITY_TYPE.get(id);
        if (type == null) {
            throw new JsonSyntaxException("Unknown tile entity '" + id + "'");
        }
        return new Instance(entityPredicate, type);
    }

    /**
     * Triggers this criteria
     */
    public void trigger(@Nullable BlockEntity tileEntity, @Nullable PlayerInventory inv) {
        if (tileEntity != null && inv != null && inv.player instanceof ServerPlayerEntity) {
            this.trigger((ServerPlayerEntity) inv.player, instance -> instance.test(tileEntity.getType()));
        }
    }

    public static class Instance extends AbstractCriterionConditions {
        private final BlockEntityType<?> type;

        public Instance(Composite playerCondition, BlockEntityType<?> type) {
            super(ID, playerCondition);
            this.type = type;
        }

        public static Instance container(BlockEntityType<?> type) {
            return new Instance(Composite.ANY, type);
        }

        /**
         * Tests if this instance matches
         */
        public boolean test(BlockEntityType<?> type) {
            return this.type == type;
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer conditions) {
            JsonObject json = super.toJson(conditions);
            json.addProperty("type", Objects.requireNonNull(Registries.BLOCK_ENTITY_TYPE.getId(type)).toString());
            return json;
        }
    }
}
