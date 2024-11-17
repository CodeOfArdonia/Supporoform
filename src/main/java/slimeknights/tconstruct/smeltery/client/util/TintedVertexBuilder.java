package slimeknights.tconstruct.smeltery.client.util;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.render.VertexConsumer;

/**
 * Vertex builder wrapper that tints all quads passed in
 */
@RequiredArgsConstructor
public class TintedVertexBuilder implements VertexConsumer {
    /**
     * Base vertex builder
     */
    private final VertexConsumer inner;
    /**
     * Tint color from 0-255
     */
    private final int tintRed, tintGreen, tintBlue, tintAlpha;

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        return this.inner.vertex(x, y, z);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return this.inner.color((red * this.tintRed) / 0xFF, (green * this.tintGreen) / 0xFF, (blue * this.tintBlue) / 0xFF, (alpha * this.tintAlpha) / 0xFF);
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha) {
        // TODO: is setting the default color here correct?
        this.inner.fixedColor((red * this.tintRed) / 0xFF, (green * this.tintGreen) / 0xFF, (blue * this.tintBlue) / 0xFF, (alpha * this.tintAlpha) / 0xFF);
    }

    @Override
    public void unfixColor() {
        this.inner.unfixColor();
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return this.inner.texture(u, v);
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return this.inner.overlay(u, v);
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return this.inner.light(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return this.inner.normal(x, y, z);
    }

    @Override
    public void next() {
        this.inner.next();
    }
}
