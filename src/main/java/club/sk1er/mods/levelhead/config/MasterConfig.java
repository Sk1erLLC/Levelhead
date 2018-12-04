package club.sk1er.mods.levelhead.config;

public class MasterConfig {

    private boolean enabled = true;
    private boolean showSelf = true;
    private int renderDistance = 64;

    private int purgeSize = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isShowSelf() {
        return showSelf;
    }

    public void setShowSelf(boolean showSelf) {
        this.showSelf = showSelf;
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public void setRenderDistance(int renderDistance) {
        this.renderDistance = renderDistance;
    }

    public int getPurgeSize() {
        return purgeSize;
    }

    public void setPurgeSize(int purgeSize) {
        this.purgeSize = purgeSize;
    }
}
