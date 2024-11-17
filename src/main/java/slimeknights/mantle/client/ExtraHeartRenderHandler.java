package slimeknights.mantle.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.config.Config;

import java.util.Random;

public class ExtraHeartRenderHandler {
    private static final Identifier ICON_HEARTS = new Identifier(Mantle.modId, "textures/gui/hearts.png");
    private static final Identifier ICON_ABSORB = new Identifier(Mantle.modId, "textures/gui/absorb.png");
    private static final Identifier ICON_VANILLA = InGameHud.ICONS;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private int playerHealth = 0;
    private int lastPlayerHealth = 0;
    private long healthUpdateCounter = 0;
    private long lastSystemTime = 0;
    private final Random rand = new Random();

    private int regen;

    /* HUD */

    /**
     * Event listener
     *
     * @param event Event instance
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void renderHealthbar(RenderGuiOverlayEvent.Pre event) {
        if (event.isCanceled() || !Config.EXTRA_HEART_RENDERER.get() || event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            return;
        }
        // ensure its visible
        if (!(this.mc.inGameHud instanceof ForgeGui gui) || this.mc.options.hudHidden || !gui.shouldDrawSurvivalElements()) {
            return;
        }
        Entity renderViewEnity = this.mc.getCameraEntity();
        if (!(renderViewEnity instanceof PlayerEntity player)) {
            return;
        }
        gui.setupOverlayRenderState(true, false);

        this.mc.getProfiler().push("health");

        // extra setup stuff from us
        int left_height = gui.leftHeight;
        int width = this.mc.getWindow().getScaledWidth();
        int height = this.mc.getWindow().getScaledHeight();
        int updateCounter = this.mc.inGameHud.getTicks();

        // start default forge/mc rendering
        // changes are indicated by comment

        int health = MathHelper.ceil(player.getHealth());
        boolean highlight = this.healthUpdateCounter > (long) updateCounter && (this.healthUpdateCounter - (long) updateCounter) / 3L % 2L == 1L;

        if (health < this.playerHealth && player.timeUntilRegen > 0) {
            this.lastSystemTime = Util.getMeasuringTimeMs();
            this.healthUpdateCounter = (updateCounter + 20);
        } else if (health > this.playerHealth && player.timeUntilRegen > 0) {
            this.lastSystemTime = Util.getMeasuringTimeMs();
            this.healthUpdateCounter = (updateCounter + 10);
        }

        if (Util.getMeasuringTimeMs() - this.lastSystemTime > 1000L) {
            this.playerHealth = health;
            this.lastPlayerHealth = health;
            this.lastSystemTime = Util.getMeasuringTimeMs();
        }

        this.playerHealth = health;
        int healthLast = this.lastPlayerHealth;

        EntityAttributeInstance attrMaxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        float healthMax = attrMaxHealth == null ? 0 : (float) attrMaxHealth.getValue();
        float absorb = MathHelper.ceil(player.getAbsorptionAmount());

        // CHANGE: simulate 10 hearts max if there's more, so vanilla only renders one row max
        healthMax = Math.min(healthMax, 20f);
        health = Math.min(health, 20);
        absorb = Math.min(absorb, 20);

        int healthRows = MathHelper.ceil((healthMax + absorb) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        this.rand.setSeed(updateCounter * 312871L);

        int left = width / 2 - 91;
        int top = height - left_height;
        // change: these are unused below, unneeded? should these adjust the Forge variable?
        //left_height += (healthRows * rowHeight);
        //if (rowHeight != 10) left_height += 10 - rowHeight;

        this.regen = -1;
        if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
            this.regen = updateCounter % 25;
        }

        assert this.mc.world != null;
        final int TOP = 9 * (this.mc.world.getLevelProperties().isHardcore() ? 5 : 0);
        final int BACKGROUND = (highlight ? 25 : 16);
        int MARGIN = 16;
        if (player.hasStatusEffect(StatusEffects.POISON)) MARGIN += 36;
        else if (player.hasStatusEffect(StatusEffects.WITHER)) MARGIN += 72;
        float absorbRemaining = absorb;

        DrawContext graphics = event.getGuiGraphics();
        for (int i = MathHelper.ceil((healthMax + absorb) / 2.0F) - 1; i >= 0; --i) {
            int row = MathHelper.ceil((float) (i + 1) / 10.0F) - 1;
            int x = left + i % 10 * 8;
            int y = top - row * rowHeight;

            if (health <= 4) y += this.rand.nextInt(2);
            if (i == this.regen) y -= 2;

            graphics.drawTexture(ICON_VANILLA, x, y, BACKGROUND, TOP, 9, 9);

            if (highlight) {
                if (i * 2 + 1 < healthLast) {
                    graphics.drawTexture(ICON_VANILLA, x, y, MARGIN + 54, TOP, 9, 9); //6
                } else if (i * 2 + 1 == healthLast) {
                    graphics.drawTexture(ICON_VANILLA, x, y, MARGIN + 63, TOP, 9, 9); //7
                }
            }

            if (absorbRemaining > 0.0F) {
                if (absorbRemaining == absorb && absorb % 2.0F == 1.0F) {
                    graphics.drawTexture(ICON_VANILLA, x, y, MARGIN + 153, TOP, 9, 9); //17
                    absorbRemaining -= 1.0F;
                } else {
                    graphics.drawTexture(ICON_VANILLA, x, y, MARGIN + 144, TOP, 9, 9); //16
                    absorbRemaining -= 2.0F;
                }
            } else {
                if (i * 2 + 1 < health) {
                    graphics.drawTexture(ICON_VANILLA, x, y, MARGIN + 36, TOP, 9, 9); //4
                } else if (i * 2 + 1 == health) {
                    graphics.drawTexture(ICON_VANILLA, x, y, MARGIN + 45, TOP, 9, 9); //5
                }
            }
        }

        this.renderExtraHearts(graphics, left, top, player);
        this.renderExtraAbsorption(graphics, left, top - rowHeight, player);

        RenderSystem.setShaderTexture(0, ICON_VANILLA);
        gui.leftHeight += 10;
        if (absorb > 0) {
            gui.leftHeight += 10;
        }

        event.setCanceled(true);
        RenderSystem.disableBlend();
        this.mc.getProfiler().pop();
        //noinspection UnstableApiUsage  I do what I want (more accurately, we override the renderer but want to let others still respond in post)
        MinecraftForge.EVENT_BUS.post(new RenderGuiOverlayEvent.Post(event.getWindow(), graphics, event.getPartialTick(), VanillaGuiOverlay.PLAYER_HEALTH.type()));
    }

    /**
     * Gets the texture from potion effects
     *
     * @param player Player instance
     * @return Texture offset for potion effects
     */
    private int getPotionOffset(PlayerEntity player) {
        int potionOffset = 0;
        StatusEffectInstance potion = player.getStatusEffect(StatusEffects.WITHER);
        if (potion != null) {
            potionOffset = 18;
        }
        potion = player.getStatusEffect(StatusEffects.POISON);
        if (potion != null) {
            potionOffset = 9;
        }
        assert this.mc.world != null;
        if (this.mc.world.getLevelProperties().isHardcore()) {
            potionOffset += 27;
        }
        return potionOffset;
    }

    /**
     * Renders the health above 10 hearts
     *
     * @param graphics Graphics instance
     * @param xBasePos Health bar top corner
     * @param yBasePos Health bar top corner
     * @param player   Player instance
     */
    private void renderExtraHearts(DrawContext graphics, int xBasePos, int yBasePos, PlayerEntity player) {
        int potionOffset = this.getPotionOffset(player);

        // Extra hearts
//    RenderSystem.setShaderTexture(0, ICON_HEARTS);
        int hp = MathHelper.ceil(player.getHealth());
        this.renderCustomHearts(graphics, ICON_HEARTS, xBasePos, yBasePos, potionOffset, hp, false);
    }

    /**
     * Renders the absorption health above 10 hearts
     *
     * @param graphics Graphics instance
     * @param xBasePos Health bar top corner
     * @param yBasePos Health bar top corner
     * @param player   Player instance
     */
    private void renderExtraAbsorption(DrawContext graphics, int xBasePos, int yBasePos, PlayerEntity player) {
        int potionOffset = this.getPotionOffset(player);

        // Extra hearts
//    RenderSystem.setShaderTexture(0, ICON_ABSORB);
        int absorb = MathHelper.ceil(player.getAbsorptionAmount());
        this.renderCustomHearts(graphics, ICON_ABSORB, xBasePos, yBasePos, potionOffset, absorb, true);
    }

    /**
     * Gets the texture offset from the regen effect
     *
     * @param i      Heart index
     * @param offset Current offset
     */
    private int getYRegenOffset(int i, int offset) {
        return i + offset == this.regen ? -2 : 0;
    }

    /**
     * Shared logic to render custom hearts
     *
     * @param graphics     Graphics instance
     * @param texture      Texture for drawing the hearts
     * @param xBasePos     Health bar top corner
     * @param yBasePos     Health bar top corner
     * @param potionOffset Offset from the potion effect
     * @param count        Number to render
     * @param absorb       If true, render absorption hearts
     */
    private void renderCustomHearts(DrawContext graphics, Identifier texture, int xBasePos, int yBasePos, int potionOffset, int count, boolean absorb) {
        int regenOffset = absorb ? 10 : 0;
        for (int iter = 0; iter < count / 20; iter++) {
            int renderHearts = (count - 20 * (iter + 1)) / 2;
            int heartIndex = iter % 11;
            if (renderHearts > 10) {
                renderHearts = 10;
            }
            for (int i = 0; i < renderHearts; i++) {
                int y = this.getYRegenOffset(i, regenOffset);
                if (absorb) {
                    graphics.drawTexture(texture, xBasePos + 8 * i, yBasePos + y, 0, 54, 9, 9);
                }
                graphics.drawTexture(texture, xBasePos + 8 * i, yBasePos + y, 18 * heartIndex, potionOffset, 9, 9);
            }
            if (count % 2 == 1 && renderHearts < 10) {
                int y = this.getYRegenOffset(renderHearts, regenOffset);
                if (absorb) {
                    graphics.drawTexture(texture, xBasePos + 8 * renderHearts, yBasePos + y, 0, 54, 9, 9);
                }
                graphics.drawTexture(texture, xBasePos + 8 * renderHearts, yBasePos + y, 9 + 18 * heartIndex, potionOffset, 9, 9);
            }
        }
    }
}
