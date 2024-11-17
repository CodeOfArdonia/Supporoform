package slimeknights.mantle.client.book.data.element;

import net.minecraft.text.Text;

public class TextComponentData {

    public static final TextComponentData LINEBREAK = new TextComponentData("\n");

    public Text text;

    public boolean isParagraph = false;
    public boolean dropShadow = false;
    public float scale = 1.F;
    public String action = "";
    public Text[] tooltips = null;

    public TextComponentData(String text) {
        this(Text.literal(text));
    }

    public TextComponentData(Text text) {
        this.text = text;
    }


}
