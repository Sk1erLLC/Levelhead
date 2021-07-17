package club.sk1er.mods.levelhead.renderer;

public class LevelheadComponent {
    private String value;
    //chroma
    private boolean chroma;
    //Basic color code color
    private String color;
    //RGBA
    private boolean rgb;
    private int red;
    private int green;
    private int blue;
    private int alpha;
    private boolean custom;

    public LevelheadComponent(String value) {
        this.value = value.replace("&", "\u00a7");
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {

        this.color = color;
    }

    public boolean isRgb() {
        return rgb;
    }

    public void setRgb(boolean rgb) {
        this.rgb = rgb;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isChroma() {
        return chroma;
    }

    public void setChroma(boolean chroma) {
        this.chroma = chroma;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    @Override
    public String toString() {
        return "LevelheadComponent{" +
            "value='" + value + '\'' +
            ", chroma=" + chroma +
            ", color='" + color + '\'' +
            ", rgb=" + rgb +
            ", red=" + red +
            ", green=" + green +
            ", blue=" + blue +
            ", alpha=" + alpha +
            ", custom=" + custom +
            '}';
    }
}
