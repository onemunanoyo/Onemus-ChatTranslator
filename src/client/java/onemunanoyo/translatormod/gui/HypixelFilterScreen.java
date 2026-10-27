package onemunanoyo.translatormod.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.util.I18n;

public class HypixelFilterScreen extends Screen {

    private final Screen parent;
    private TranslatorConfig config;

    private static final int PADDING = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;

    private int panelWidth;
    private int panelHeight;
    private int panelX;
    private int panelY;
    private int contentX;
    private int contentWidth;

    public HypixelFilterScreen(Screen parent) {
        super(I18n.translate("translatormod.hypixel.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        config = TranslatorConfig.getInstance();

        int maxPanelW = Math.max(320, this.width  - 4);
        int maxPanelH = Math.max(200, this.height - 4);
        panelWidth  = Math.clamp((int)(this.width  * 0.6), Math.min(320, maxPanelW), maxPanelW);
        panelHeight = Math.clamp((int)(this.height * 0.6), Math.min(200, maxPanelH), Math.min(400, maxPanelH));
        panelX = (this.width  - panelWidth)  / 2;
        panelY = (this.height - panelHeight) / 2;
        contentX     = panelX + PADDING;
        contentWidth = panelWidth - PADDING * 2;

        int y = panelY + PADDING + 14 + PADDING;

        // Hypixel モード ON/OFF
        this.addDrawableChild(ButtonWidget.builder(
                getHypixelModeText(),
                btn -> {
                    config.hypixelMode = !config.hypixelMode;
                    config.save();
                    btn.setMessage(getHypixelModeText());
                }
        ).dimensions(contentX, y, contentWidth, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + BUTTON_GAP * 3;

        // Party
        this.addDrawableChild(ButtonWidget.builder(
                getChannelText(I18n.getString("translatormod.hypixel.channel.party"), config.hypixelParty),
                btn -> {
                    config.hypixelParty = nextMode(config.hypixelParty);
                    config.save();
                    btn.setMessage(getChannelText(I18n.getString("translatormod.hypixel.channel.party"), config.hypixelParty));
                }
        ).dimensions(contentX, y, contentWidth, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + BUTTON_GAP;

        // Guild
        this.addDrawableChild(ButtonWidget.builder(
                getChannelText(I18n.getString("translatormod.hypixel.channel.guild"), config.hypixelGuild),
                btn -> {
                    config.hypixelGuild = nextMode(config.hypixelGuild);
                    config.save();
                    btn.setMessage(getChannelText(I18n.getString("translatormod.hypixel.channel.guild"), config.hypixelGuild));
                }
        ).dimensions(contentX, y, contentWidth, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + BUTTON_GAP;

        // DM
        this.addDrawableChild(ButtonWidget.builder(
                getChannelText(I18n.getString("translatormod.hypixel.channel.dm"), config.hypixelDM),
                btn -> {
                    config.hypixelDM = nextMode(config.hypixelDM);
                    config.save();
                    btn.setMessage(getChannelText(I18n.getString("translatormod.hypixel.channel.dm"), config.hypixelDM));
                }
        ).dimensions(contentX, y, contentWidth, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + BUTTON_GAP;

        // All Chat
        this.addDrawableChild(ButtonWidget.builder(
                getChannelText(I18n.getString("translatormod.hypixel.channel.all"), config.hypixelAllChat),
                btn -> {
                    config.hypixelAllChat = nextMode(config.hypixelAllChat);
                    config.save();
                    btn.setMessage(getChannelText(I18n.getString("translatormod.hypixel.channel.all"), config.hypixelAllChat));
                }
        ).dimensions(contentX, y, contentWidth, BUTTON_HEIGHT).build());

        // 戻るボタン
        int backY = panelY + panelHeight - BUTTON_HEIGHT - PADDING;
        this.addDrawableChild(ButtonWidget.builder(
                I18n.translate("translatormod.button.back"),
                btn -> this.close()
        ).dimensions(contentX, backY, contentWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF555555);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF555555);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelY + PADDING, 0xFFFFFFFF);

        // Hypixel モードが OFF のときグレーアウト
        if (!config.hypixelMode) {
            int overlayY = panelY + PADDING + 14 + PADDING + BUTTON_HEIGHT + BUTTON_GAP * 3;
            int overlayBottom = overlayY + (BUTTON_HEIGHT + BUTTON_GAP) * 4 - BUTTON_GAP;
            context.fill(contentX, overlayY, contentX + contentWidth, overlayBottom, 0x88000000);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** OFF → AUTO → CLICK → OFF のサイクル */
    private static String nextMode(String current) {
        return switch (current) {
            case "OFF"   -> "AUTO";
            case "AUTO"  -> "CLICK";
            default      -> "OFF";
        };
    }

    private Text getHypixelModeText() {
        return config.hypixelMode
                ? I18n.translate("translatormod.hypixel.toggle_on")
                : I18n.translate("translatormod.hypixel.toggle_off");
    }

    private Text getChannelText(String label, String mode) {
        return switch (mode) {
            case "AUTO"  -> I18n.translate("translatormod.hypixel.mode.auto", label);
            case "CLICK" -> I18n.translate("translatormod.hypixel.mode.click", label);
            default      -> I18n.translate("translatormod.hypixel.mode.off", label);
        };
    }
}