package slimeknights.mantle.client.screen.book;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.data.element.TextData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TextDataRenderer {

    /**
     * @deprecated Call drawText with tooltip param and then call drawTooltip separately on the tooltip layer to prevent overlap
     */
    @Deprecated
    public static String drawText(DrawContext graphics, int x, int y, int boxWidth, int boxHeight, TextData[] data, int mouseX, int mouseY, TextRenderer fr, BookScreen parent) {
        List<Text> tooltip = new ArrayList<>();
        String action = drawText(graphics, x, y, boxWidth, boxHeight, data, mouseX, mouseY, fr, tooltip);

        if (tooltip.size() > 0) {
            graphics.drawTooltip(fr, tooltip, Optional.empty(), mouseX, mouseY);
        }

        return action;
    }

    public static String drawText(DrawContext graphics, int x, int y, int boxWidth, int boxHeight, TextData[] data, int mouseX, int mouseY, TextRenderer fr, List<Text> tooltip) {
        String action = "";

        int atX = x;
        int atY = y;

        float prevScale = 1.F;

        for (TextData item : data) {
            int box1X, box1Y, box1W = 9999, box1H = y + fr.fontHeight;
            int box2X, box2Y = 9999, box2W, box2H;
            int box3X = 9999, box3Y = 9999, box3W, box3H;

            if (item == null || item.text == null || item.text.isEmpty()) {
                continue;
            }
            if (item.text.equals("\n")) {
                atX = x;
                atY += fr.fontHeight;
                continue;
            }

            if (item.paragraph) {
                atX = x;
                atY += fr.fontHeight * 2 * prevScale;
            }

            prevScale = item.scale;

            String modifiers = "";

            if (item.useOldColor) {
                Formatting colFormat = Formatting.byName(item.color);
                if (colFormat != null) {
                    modifiers += colFormat;
                } else {
                    modifiers += "unknown color"; // more descriptive than null

                    // This will spam the console, but that makes the error more obvious
                    Mantle.logger.error("Failed to parse color: " + item.color + " for text rendering.");
                }
            }

            if (item.bold) {
                modifiers += Formatting.BOLD;
            }
            if (item.italic) {
                modifiers += Formatting.ITALIC;
            }
            if (item.underlined) {
                modifiers += Formatting.UNDERLINE;
            }
            if (item.strikethrough) {
                modifiers += Formatting.STRIKETHROUGH;
            }
            if (item.obfuscated) {
                modifiers += Formatting.OBFUSCATED;
            }

            String text = translateString(item.text);

            String[] split = cropStringBySize(text, modifiers, boxWidth, boxHeight - (atY - y), boxWidth - (atX - x), fr, item.scale);

            box1X = atX;
            box1Y = atY;
            box2X = x;
            box2W = x + boxWidth;

            for (int i = 0; i < split.length; i++) {
                if (i == split.length - 1) {
                    box3X = atX;
                    box3Y = atY;
                }

                String s = split[i];
                drawScaledString(graphics, fr, modifiers + s, atX, atY, item.rgbColor, item.dropshadow, item.scale);

                if (i < split.length - 1) {
                    atY += fr.fontHeight;
                    atX = x;
                }

                if (i == 0) {
                    box2Y = atY;

                    if (atX == x) {
                        box1W = x + boxWidth;
                    } else {
                        box1W = atX;
                    }
                }
            }

            box2H = atY;

            atX += fr.getWidth(split[split.length - 1]) * item.scale;
            if (atX - x >= boxWidth) {
                atX = x;
                atY += fr.fontHeight * item.scale;
            }

            box3W = atX;
            box3H = (int) (atY + fr.fontHeight * item.scale);

            boolean mouseInside = (mouseX >= box1X && mouseX <= box1W && mouseY >= box1Y && mouseY <= box1H && box1X != box1W && box1Y != box1H)
                    || (mouseX >= box2X && mouseX <= box2W && mouseY >= box2Y && mouseY <= box2H && box2X != box2W && box2Y != box2H)
                    || (mouseX >= box3X && mouseX <= box3W && mouseY >= box3Y && mouseY <= box3H && box3X != box3W && box1Y != box3H);
            if (item.tooltip != null && item.tooltip.length > 0) {
                if (BookScreen.debug) {
                    graphics.fillGradient(box1X, box1Y, box1W, box1H, 0xFF00FF00, 0xFF00FF00);
                    graphics.fillGradient(box2X, box2Y, box2W, box2H, 0xFFFF0000, 0xFFFF0000);
                    graphics.fillGradient(box3X, box3Y, box3W, box3H, 0xFF0000FF, 0xFF0000FF);
                    graphics.fillGradient(mouseX, mouseY, mouseX + 5, mouseY + 5, 0xFFFF00FF, 0xFFFFFF00);
                }

                if (mouseInside) {
                    tooltip.addAll(Arrays.asList(item.tooltip));
                }
            }

            if (item.action != null && !item.action.isEmpty()) {
                if (mouseInside) {
                    action = item.action;
                }
            }

            if (atY >= y + boxHeight) {
                graphics.drawText(fr, "...", atX, atY, 0, item.dropshadow);
                break;
            }
            y = atY;
        }

        if (BookScreen.debug && !action.isEmpty()) {
            tooltip.add(Text.empty());
            tooltip.add(Text.literal("Action: " + action).formatted(Formatting.GRAY));
        }

        return action;
    }

    public static String translateString(String s) {
        s = s.replace("$$(", "$\0(").replace(")$$", ")\0$");

        while (s.contains("$(") && s.contains(")$") && s.indexOf("$(") < s.indexOf(")$")) {
            String loc = s.substring(s.indexOf("$(") + 2, s.indexOf(")$"));
            s = s.replace("$(" + loc + ")$", I18n.translate(loc));
        }

        if (s.indexOf("$(") > s.indexOf(")$") || s.contains(")$")) {
            Mantle.logger.error("[Books] [TextDataRenderer] Detected unbalanced localization symbols \"$(\" and \")$\" in string: \"" + s + "\".");
        }

        return s.replace("$\0(", "$(").replace(")\0$", ")$");
    }

    public static String[] cropStringBySize(String s, String modifiers, int width, int height, TextRenderer fr, float scale) {
        return cropStringBySize(s, modifiers, width, height, width, fr, scale);
    }

    public static String[] cropStringBySize(String s, String modifiers, int width, int height, int firstWidth, TextRenderer fr, float scale) {
        int curWidth = 0;
        int curHeight = (int) (fr.fontHeight * scale);

        for (int i = 0; i < s.length(); i++) {
            curWidth += fr.getWidth(modifiers + s.charAt(i)) * scale;

            if (s.charAt(i) == '\n' || (curHeight == (int) (fr.fontHeight * scale) ? curWidth > firstWidth : curWidth > width)) {
                int oldI = i;
                if (s.charAt(i) != '\n') {
                    while (i >= 0 && s.charAt(i) != ' ') {
                        i--;
                    }
                    if (i <= 0) {
                        i = oldI;
                    }
                } else {
                    oldI++;
                }

                s = s.substring(0, i) + "\r" + StringUtils.stripStart(s.substring(i + (i == oldI ? 0 : 1)), " ");

                i++;
                curWidth = 0;
                curHeight += fr.fontHeight * scale;

                if (curHeight >= height) {
                    return s.substring(0, i).split("\r");
                }
            }
        }

        return s.split("\r");
    }

    /**
     * Gets the number of lines needed to render the given text
     */
    public static int getLinesForString(String s, String modifiers, int width, String prefix, TextRenderer fr) {
        return cropStringBySize(s, modifiers, width, Short.MAX_VALUE, width - fr.getWidth(prefix), fr, 1.0f).length;
    }

    //BEGIN METHODS FROM GUI
    //TODO: does this exist elsewhere now?
    public static void drawScaledString(DrawContext graphics, TextRenderer font, String text, float x, float y, int color, boolean dropShadow, float scale) {
        MatrixStack poseStack = graphics.getMatrices();
        poseStack.push();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1F);

        graphics.drawText(font, text, 0, 0, color, dropShadow);

        poseStack.pop();
    }
    //END METHODS FROM GUI
}
