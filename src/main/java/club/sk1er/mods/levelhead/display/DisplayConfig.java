package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.core.universal.ChatColor;

/**
 * Created by mitchellkatz on 12/23/17. Designed for production use on Sk1er.club
 */
public class DisplayConfig {


    private boolean headerChroma = false;
    private boolean headerRgb = false;
    private String headerColor = ChatColor.AQUA.toString();
    private int headerRed = 255;
    private int headerGreen = 255;
    private int headerBlue = 250;
    private double headerAlpha = 1.0;
    private String customHeader = "Level";
    private boolean enable = true;
    private boolean footerChroma = false;
    private boolean footerRgb = false;
    private String footerColor = ChatColor.YELLOW.toString();
    private int footerRed = 255;
    private int footerGreen = 255;
    private int footerBlue = 250;
    private double footerAlpha = 1.0;
    private String type = "LEVEL";
    private boolean showSelf = true;


    public boolean isShowSelf() {
        return showSelf;
    }

    public void setShowSelf(boolean showSelf) {
        this.showSelf = showSelf;
    }

    public boolean isEnabled() {
        return enable;
    }

    public void setEnabled(boolean enable) {
        this.enable = enable;
    }

    public boolean isFooterChroma() {
        return footerChroma;
    }

    public void setFooterChroma(boolean footerChroma) {
        this.footerChroma = footerChroma;
    }

    public boolean isFooterRgb() {
        return footerRgb;
    }

    public void setFooterRgb(boolean footerRgb) {
        this.footerRgb = footerRgb;
    }

    public String getFooterColor() {
        return footerColor;
    }

    public void setFooterColor(String footerColor) {
        this.footerColor = footerColor;
    }

    public int getFooterRed() {
        return footerRed;
    }

    public void setFooterRed(int footerRed) {
        this.footerRed = footerRed;
    }

    public int getFooterGreen() {
        return footerGreen;
    }

    public void setFooterGreen(int footerGreen) {
        this.footerGreen = footerGreen;
    }

    public int getFooterBlue() {
        return footerBlue;
    }

    public void setFooterBlue(int footerBlue) {
        this.footerBlue = footerBlue;
    }

    public double getFooterAlpha() {
        return footerAlpha;
    }

    public void setFooterAlpha(double footerAlpha) {
        this.footerAlpha = footerAlpha;
    }

    public boolean isHeaderChroma() {
        return headerChroma;
    }

    public void setHeaderChroma(boolean headerChroma) {
        this.headerChroma = headerChroma;
    }

    public boolean isHeaderRgb() {
        return headerRgb;
    }

    public void setHeaderRgb(boolean headerRgb) {
        this.headerRgb = headerRgb;
    }

    public String getHeaderColor() {
        return headerColor;
    }

    public void setHeaderColor(String headerColor) {
        this.headerColor = headerColor;
    }

    public int getHeaderRed() {
        return headerRed;
    }

    public void setHeaderRed(int headerRed) {
        this.headerRed = headerRed;
    }

    public int getHeaderGreen() {
        return headerGreen;
    }

    public void setHeaderGreen(int headerGreen) {
        this.headerGreen = headerGreen;
    }

    public int getHeaderBlue() {
        return headerBlue;
    }

    public void setHeaderBlue(int headerBlue) {
        this.headerBlue = headerBlue;
    }

    public double getHeaderAlpha() {
        return headerAlpha;
    }

    public void setHeaderAlpha(double headerAlpha) {
        this.headerAlpha = headerAlpha;
    }

    public String getCustomHeader() {
        return customHeader;
    }

    public void setCustomHeader(String customHeader) {
        this.customHeader = customHeader;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
