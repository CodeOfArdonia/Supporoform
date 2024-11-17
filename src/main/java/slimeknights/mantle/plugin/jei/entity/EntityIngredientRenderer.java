package slimeknights.mantle.plugin.jei.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.RequiredArgsConstructor;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.ingredient.EntityIngredient;

import java.util.*;

/**
 * Renderer for entity type ingredients
 */
@RequiredArgsConstructor
public class EntityIngredientRenderer implements IIngredientRenderer<EntityIngredient.EntityInput> {
    private static final Identifier MISSING = Mantle.getResource("textures/item/missingno.png");
    /**
     * Entity types that will not render, as they either errored or are the wrong type
     */
    private static final Set<EntityType<?>> IGNORED_ENTITIES = new HashSet<>();

    /**
     * Square size of the renderer in pixels
     */
    private final int size;

    /**
     * Cache of entities for each entity type
     */
    private final Map<EntityType<?>, Entity> ENTITY_MAP = new HashMap<>();

    @Override
    public int getWidth() {
        return this.size;
    }

    @Override
    public int getHeight() {
        return this.size;
    }

    @Override
    public void render(DrawContext graphics, @Nullable EntityIngredient.EntityInput input) {
        if (input != null) {
            World world = MinecraftClient.getInstance().world;
            EntityType<?> type = input.type();
            if (world != null && !IGNORED_ENTITIES.contains(type)) {
                Entity entity;
                // players cannot be created using the type, but we can use the client player
                // side effect is it renders armor/items
                if (type == EntityType.PLAYER) {
                    entity = MinecraftClient.getInstance().player;
                } else {
                    // entity is created with the client world, but the entity map is thrown away when JEI restarts so they should be okay I think
                    entity = this.ENTITY_MAP.computeIfAbsent(type, t -> t.create(world));
                }
                // only can draw living entities, plus non-living ones don't get recipes anyways
                if (entity instanceof LivingEntity livingEntity) {
                    // scale down large mobs, but don't scale up small ones
                    int scale = this.size / 2;
                    float height = entity.getHeight();
                    float width = entity.getWidth();
                    if (height > 2 || width > 2) {
                        scale = (int) (this.size / Math.max(height, width));
                    }
                    // catch exceptions drawing the entity to be safe, any caught exceptions blacklist the entity
                    try {
                        InventoryScreen.drawEntity(graphics, this.size / 2, this.size, scale, 0, 10, livingEntity);
                        return;
                    } catch (Exception e) {
                        Mantle.logger.error("Error drawing entity " + Registries.ENTITY_TYPE.getId(type), e);
                        IGNORED_ENTITIES.add(type);
                        this.ENTITY_MAP.remove(type);
                    }
                } else {
                    // not living, so might as well skip next time
                    IGNORED_ENTITIES.add(type);
                    this.ENTITY_MAP.remove(type);
                }
            }

            // fallback, draw a pink and black "spawn egg"
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            int offset = (this.size - 16) / 2;
            graphics.drawTexture(MISSING, offset, offset, 0, 0, 16, 16, 16, 16);
        }
    }

    @Override
    public List<Text> getTooltip(EntityIngredient.EntityInput type, TooltipContext flag) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(type.type().getName());
        if (flag.isAdvanced()) {
            tooltip.add((Text.literal(Registries.ENTITY_TYPE.getId(type.type()).toString())).formatted(Formatting.DARK_GRAY));
        }
        return tooltip;
    }
}
