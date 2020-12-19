package club.sk1er.mods.levelhead.renderer;


import net.modcore.api.utils.JsonHolder;

import java.util.UUID;

/**
 * Created by mitchellkatz on 12/22/17. Designed for production use on Levelhead
 */
public class LevelheadTag {
    private LevelheadComponent header;
    private LevelheadComponent footer;
    private final UUID owner;
    private final long time = System.currentTimeMillis();

    public LevelheadTag(UUID owner) {
        this.owner = owner;

    }

    public long getTime() {
        return time;
    }

    public LevelheadComponent getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "LevelheadTag{" +
            "header=" + header +
            ", footer=" + footer +
            ", owner=" + owner +
            '}';
    }

    public LevelheadComponent getFooter() {
        return footer;
    }

    public UUID getOwner() {
        return owner;
    }

    public void construct(JsonHolder holder) {
        if (header == null) {
            this.header = build(holder, true);
        }
        if (footer == null) {
            this.footer = build(holder, false);
        }
    }

    public void reApply(LevelheadTag holder) {
        if (!this.header.isCustom()) {
            this.header = holder.header;
        }
        if (!this.footer.isCustom()) {
            this.footer = holder.footer;

        }

    }

    private LevelheadComponent build(JsonHolder holder, boolean isHeader) {
        String seek = isHeader ? "header" : "footer";
        JsonHolder json = holder.optJSONObject(seek);

        LevelheadComponent component = new LevelheadComponent(json.defaultOptString(seek, "UMM BIG ERROR REPORT TO SK1ER"));
        boolean custom = json.optBoolean("custom");

        component.setCustom(custom);
        if (custom && isHeader && !holder.optBoolean("exclude")) {
            component.setValue(component.getValue() + ": ");
        }
        if (json.optBoolean("chroma")) {
            component.setChroma(true);
        } else if (json.optBoolean("rgb")) {
            component.setRgb(true);
            component.setAlpha(json.optInt("alpha"));
            component.setRed(json.optInt("red"));
            component.setBlue(json.optInt("blue"));
            component.setGreen(json.optInt("green"));
        } else {
            component.setColor(json.optString("color"));
        }

        return component;
    }

    public String getString() {
        return header.getValue() + footer.getValue();
    }

    public boolean isCustom() {
        return footer.isCustom() || header.isCustom();
    }
}
