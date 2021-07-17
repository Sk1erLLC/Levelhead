package club.sk1er.mods.levelhead.config;

public class MasterConfig {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private double fontSize = 1.0;
    private double offset = 0.0;

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public int getRenderDistance() {
        return 64;
    }

    public int getPurgeSize() {
        return 500;
    }
}
