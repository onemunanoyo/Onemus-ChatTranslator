package onemunanoyo.translatormod.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import onemunanoyo.translatormod.TranslationHandler;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.util.I18n;
import onemunanoyo.translatormod.util.DebugLog;

import java.util.ArrayList;
import java.util.List;

public class TranslatorScreen extends Screen {

    private final Screen parent;
    private TranslatorConfig config;

    private enum Tab { BASIC, RULES, IGNORE, LANGUAGE }
    private Tab currentTab = Tab.BASIC;

    private EditBox apiKeyField;
    private EditBox ignoreWordField;

    private static final int PADDING = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_HEIGHT = 20;
    private static final int TAB_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 14;
    private static final int FOOTER_HEIGHT = BUTTON_HEIGHT + PADDING * 2;
    private static final int ENTRY_HEIGHT = 22;

    private int panelWidth;
    private int panelHeight;
    private int panelX;
    private int panelY;
    private int contentX;
    private int contentWidth;

    // スクロール用
    private int scrollOffset = 0;
    private int listTop;
    private int listBottom;
    private final List<String> listEntries = new ArrayList<>();
    private final List<Runnable> listActions = new ArrayList<>();

    // 使用量確認ボタン
    private Button usageButton = null;

    private Component translatorTitle = Component.empty();

    public TranslatorScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    private void refreshTitle() {
        this.translatorTitle = I18n.translate("translatormod.title");
    }

    @Override
    protected void init() {
        config = TranslatorConfig.getInstance();
        scrollOffset = 0;
        refreshTitle();

        int maxPanelW = Math.max(320, this.width  - 4);
        int maxPanelH = Math.max(200, this.height - 4);
        panelWidth  = Math.clamp((int)(this.width  * 0.75), Math.min(320, maxPanelW), maxPanelW);
        panelHeight = Math.clamp((int)(this.height * 0.85), Math.min(380, maxPanelH), Math.min(520, maxPanelH));
        panelX = (this.width  - panelWidth)  / 2;
        panelY = Math.max(2, (this.height - panelHeight) / 2);
        contentX     = panelX + PADDING;
        contentWidth = panelWidth - PADDING * 2;

        int tabWidth = contentWidth / 4;
        int tabY = panelY + HEADER_HEIGHT + PADDING;

        this.addRenderableWidget(Button.builder(
                currentTab == Tab.BASIC ? I18n.translate("translatormod.tab.basic").copy().withStyle(ChatFormatting.YELLOW) : I18n.translate("translatormod.tab.basic"),
                btn -> { currentTab = Tab.BASIC; scrollOffset = 0; rebuildWidgets(); }
        ).bounds(contentX, tabY, tabWidth - 2, TAB_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                currentTab == Tab.RULES ? I18n.translate("translatormod.tab.rules").copy().withStyle(ChatFormatting.YELLOW) : I18n.translate("translatormod.tab.rules"),
                btn -> { currentTab = Tab.RULES; scrollOffset = 0; rebuildWidgets(); }
        ).bounds(contentX + tabWidth, tabY, tabWidth - 2, TAB_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                currentTab == Tab.IGNORE ? I18n.translate("translatormod.tab.ignore").copy().withStyle(ChatFormatting.YELLOW) : I18n.translate("translatormod.tab.ignore"),
                btn -> { currentTab = Tab.IGNORE; scrollOffset = 0; rebuildWidgets(); }
        ).bounds(contentX + tabWidth * 2, tabY, tabWidth - 2, TAB_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                currentTab == Tab.LANGUAGE ? I18n.translate("translatormod.tab.language").copy().withStyle(ChatFormatting.YELLOW) : I18n.translate("translatormod.tab.language"),
                btn -> { currentTab = Tab.LANGUAGE; scrollOffset = 0; rebuildWidgets(); }
        ).bounds(contentX + tabWidth * 3, tabY, tabWidth - 2, TAB_HEIGHT).build());

        int contentY = tabY + TAB_HEIGHT + PADDING + this.font.lineHeight + 4;

        switch (currentTab) {
            case BASIC    -> initBasicTab(contentX, contentY, contentWidth);
            case RULES    -> initRulesTab(contentX, contentY, contentWidth);
            case IGNORE   -> initIgnoreTab(contentX, contentY, contentWidth);
            case LANGUAGE -> initLanguageTab(contentX, contentY, contentWidth);
        }

        int closeY = panelY + panelHeight - BUTTON_HEIGHT - PADDING;
        this.addRenderableWidget(Button.builder(
                I18n.translate("translatormod.button.close"),
                btn -> this.onClose()
        ).bounds(contentX, closeY, contentWidth, BUTTON_HEIGHT).build());
    }

    private void initBasicTab(int x, int y, int w) {
        listEntries.clear();
        listActions.clear();

        this.addRenderableWidget(Button.builder(
                getToggleText(),
                btn -> {
                    if (!config.enabled && config.apiKey.isBlank()) {
                        if (minecraft != null && minecraft.player != null)
                            minecraft.player.sendOverlayMessage(I18n.translate("translatormod.command.on.no_api"));
                        return;
                    }
                    config.enabled = !config.enabled;
                    config.save();
                    btn.setMessage(getToggleText());
                }
        ).bounds(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        y += this.font.lineHeight + 4;

        apiKeyField = new EditBox(
                this.font, x, y, w - 82, FIELD_HEIGHT, I18n.translate("translatormod.label.api_key")
        );
        apiKeyField.setMaxLength(256);
        apiKeyField.setHint(I18n.translate(
                config.apiKey.isBlank() ? "translatormod.placeholder.api_key_empty" : "translatormod.placeholder.api_key_filled",
                config.apiKey.substring(Math.max(0, config.apiKey.length() - 4))
        ));
        this.addRenderableWidget(apiKeyField);

        this.addRenderableWidget(Button.builder(
                I18n.translate("translatormod.button.save"),
                btn -> {
                    String input = apiKeyField.getValue().trim();
                    if (!input.isEmpty()) {
                        int cooldown = TranslationHandler.getValidationCooldownRemaining();
                        if (cooldown > 0) {
                            if (minecraft != null && minecraft.player != null) {
                                minecraft.player.sendOverlayMessage(I18n.translate("translatormod.gui.api_key.cooldown", cooldown));
                            }
                            return;
                        }

                        btn.active = false;
                        btn.setMessage(I18n.translate("translatormod.gui.api_key.verifying"));

                        TranslationHandler.validateApiKeyAsync(input).thenAccept(success -> {
                            if (minecraft != null) {
                                minecraft.execute(() -> {
                                    btn.active = true;
                                    btn.setMessage(I18n.translate("translatormod.button.save"));

                                    if (success != null && success) {
                                        if (config.setApiKey(input)) {
                                            TranslationHandler.clearCache();
                                            apiKeyField.setValue("");
                                            apiKeyField.setHint(I18n.translate("translatormod.placeholder.api_key_filled",
                                                    config.apiKey.substring(Math.max(0, config.apiKey.length() - 4))
                                            ));
                                            if (minecraft.player != null) {
                                                minecraft.player.sendOverlayMessage(I18n.translate("translatormod.gui.api_key.verified"));
                                            }
                                        }
                                    } else {
                                        if (minecraft.player != null) {
                                            minecraft.player.sendOverlayMessage(I18n.translate("translatormod.gui.api_key.invalid"));
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
        ).bounds(x + w - 80, y, 80, BUTTON_HEIGHT).build());
        y += FIELD_HEIGHT + PADDING;

        this.addRenderableWidget(Button.builder(
                getTooltipText(),
                btn -> {
                    config.translateTooltips = !config.translateTooltips;
                    config.save();
                    btn.setMessage(getTooltipText());
                }
        ).bounds(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        this.addRenderableWidget(Button.builder(
                getSystemMsgText(),
                btn -> {
                    config.translateSystemMessages = !config.translateSystemMessages;
                    config.save();
                    btn.setMessage(getSystemMsgText());
                }
        ).bounds(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        int halfW = w / 2 - 2;
        this.addRenderableWidget(Button.builder(
                I18n.translate("translatormod.button.cache_clear"),
                btn -> TranslationHandler.clearCache()
        ).bounds(x, y, halfW, BUTTON_HEIGHT).build());

        usageButton = Button.builder(
                getUsageButtonText(),
                btn -> {
                    if (TranslationHandler.getUsageCooldownRemaining() > 0) return;
                    btn.setMessage(I18n.translate("translatormod.button.usage_loading"));
                    TranslationHandler.fetchUsageAsync().thenAccept(result -> {
                        DebugLog.log("Usage result received: " + result);
                        if (minecraft != null) {
                            minecraft.execute(() -> {
                                if (minecraft.player != null) {
                                    DebugLog.log("Sending message to player: " + minecraft.player.getName().getString());
                                    minecraft.player.sendSystemMessage(
                                            buildUsageChatMessage(result));
                                } else {
                                    DebugLog.log("Player is null! client=true");
                                }
                                if (usageButton != null) {
                                    usageButton.setMessage(getUsageButtonText());
                                }
                            });
                        } else {
                            DebugLog.log("Client is null!");
                        }
                    });
                }
        ).bounds(x + halfW + 4, y, halfW, BUTTON_HEIGHT).build();
        this.addRenderableWidget(usageButton);
        y += BUTTON_HEIGHT + PADDING;
    }

    private void initLanguageTab(int x, int y, int w) {
        this.addRenderableWidget(Button.builder(
                getLanguageButtonText(),
                btn -> {
                    config.uiLanguage = nextUiLanguage(config.uiLanguage);
                    config.save();
                    refreshTitle();
                    rebuildWidgets(); // 言語切り替え時に画面再描画
                }
        ).bounds(x, y, w, BUTTON_HEIGHT).build());
    }

    private String nextUiLanguage(String current) {
        return switch (current.toUpperCase()) {
            case "AUTO"     -> "JA_JP";
            case "JA_JP"    -> "EN_US";
            case "EN_US"    -> "ZH_HANS";
            case "ZH_HANS"  -> "FR_FR";
            case "FR_FR"    -> "ES_ES";
            case "ES_ES"    -> "PT_BR";
            case "PT_BR"    -> "KO_KR";
            case "KO_KR"    -> "TH_TH";
            case "TH_TH"    -> "DE_DE";
            case "DE_DE"    -> "VI_VN";
            default         -> "AUTO";
        };
    }

    private Component getLanguageButtonText() {
        String key = switch (config.uiLanguage.toUpperCase()) {
            case "JA_JP"    -> "translatormod.ui_lang.ja_jp";
            case "EN_US"    -> "translatormod.ui_lang.en_us";
            case "ZH_HANS"  -> "translatormod.ui_lang.zh_hans";
            case "FR_FR"    -> "translatormod.ui_lang.fr_fr";
            case "ES_ES"    -> "translatormod.ui_lang.es_es";
            case "PT_BR"    -> "translatormod.ui_lang.pt_br";
            case "KO_KR"    -> "translatormod.ui_lang.ko_kr";
            case "TH_TH"    -> "translatormod.ui_lang.th_th";
            case "DE_DE"    -> "translatormod.ui_lang.de_de";
            case "VI_VN"    -> "translatormod.ui_lang.vi_vn";
            default         -> "translatormod.ui_lang.auto";
        };
        return I18n.translate("translatormod.ui_lang.label", I18n.getString(key));
    }


    private Component buildUsageChatMessage(String result) {
        String prefix = I18n.getString("translatormod.msg.prefix");
        String errorPrefix = I18n.getString("translatormod.usage.error_prefix");
        String noApiMsg = I18n.getString("translatormod.usage.error.no_api");

        // result自体が翻訳キー（未翻訳状態）である可能性も考慮し、翻訳後の文字列でも比較する
        boolean isError = result.startsWith(errorPrefix) || result.equals(noApiMsg);

        if (isError) {
            return Component.literal(prefix + " " + result)
                    .withStyle(ChatFormatting.RED);
        }
        try {
            int pctStart = result.lastIndexOf('(') + 1;
            int pctEnd   = result.lastIndexOf('%');
            if (pctStart > 0 && pctEnd > pctStart) {
                String pctStr = result.substring(pctStart, pctEnd).trim().replace(",", ".");
                double pct = Double.parseDouble(pctStr);
                ChatFormatting color = pct >= 95.0 ? ChatFormatting.RED
                        : pct >= 80.0 ? ChatFormatting.YELLOW
                        : ChatFormatting.GREEN;
                return Component.literal(prefix + " " + result).withStyle(color);
            }
        } catch (NumberFormatException ignored) {}
        return Component.literal(prefix + " " + result).withStyle(ChatFormatting.GRAY);
    }

    private void initRulesTab(int x, int y, int w) {
        y += this.font.lineHeight + 4;

        // 受信言語選択ボタン
        this.addRenderableWidget(Button.builder(
                Component.literal(I18n.getString("translatormod.label.receive_lang") + " " + LanguageSelectScreen.getLanguageName(config.receiveTargetLang)),
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(new LanguageSelectScreen(this, true, lang -> {
                            config.receiveTargetLang = lang;
                            config.save();
                            TranslationHandler.clearCache();
                            rebuildWidgets();
                        }));
                    }
                }
        ).bounds(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING * 2;

        y += this.font.lineHeight + 4;

        // 送信言語選択ボタン
        this.addRenderableWidget(Button.builder(
                Component.literal(I18n.getString("translatormod.label.send_lang") + " " + LanguageSelectScreen.getLanguageName(config.trTargetLang)),
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(new LanguageSelectScreen(this, false, lang -> {
                            config.trTargetLang = lang;
                            config.save();
                            rebuildWidgets();
                        }));
                    }
                }
        ).bounds(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING * 2;

        this.addRenderableWidget(Button.builder(
                getHypixelButtonText(),
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(new HypixelFilterScreen(this));
                    }
                }
        ).bounds(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        this.addRenderableWidget(Button.builder(
                getWaitMsgText(),
                btn -> {
                    config.showWaitMessage = !config.showWaitMessage;
                    config.save();
                    btn.setMessage(getWaitMsgText());
                }
        ).bounds(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        listEntries.clear();
        listActions.clear();
    }

    private void initIgnoreTab(int x, int y, int w) {
        ignoreWordField = new EditBox(
                this.font, x, y, w - 52, FIELD_HEIGHT, Component.literal("ignoreWord")
        );
        ignoreWordField.setMaxLength(64);
        ignoreWordField.setHint(I18n.translate("translatormod.placeholder.ignore_word"));
        this.addRenderableWidget(ignoreWordField);

        this.addRenderableWidget(Button.builder(
                I18n.translate("translatormod.button.add"),
                btn -> {
                    String word = ignoreWordField.getValue().trim();
                    if (!word.isEmpty()) {
                        config.addIgnoreWord(word);
                        ignoreWordField.setValue("");
                        rebuildWidgets();
                    }
                }
        ).bounds(x + w - 48, y, 48, FIELD_HEIGHT).build());
        y += FIELD_HEIGHT + PADDING;

        listTop = y;
        listBottom = panelY + panelHeight - FOOTER_HEIGHT - PADDING;
        listEntries.clear();
        listActions.clear();
        for (String word : new ArrayList<>(config.ignoreWords)) {
            listEntries.add(word);
            listActions.add(() -> { config.removeIgnoreWord(word); rebuildWidgets(); });
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF555555);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF555555);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
        context.centeredText(this.font, this.translatorTitle, this.width / 2, panelY + PADDING, 0xFFFFFFFF);

        if (currentTab == Tab.BASIC) {
            if (apiKeyField != null)
                context.text(this.font, I18n.translate("translatormod.label.api_key"),
                        contentX, apiKeyField.getY() - this.font.lineHeight - 2, 0xFFAAAAAA, true);
        } else if (currentTab == Tab.IGNORE) {
            if (ignoreWordField != null)
                context.text(this.font, I18n.translate("translatormod.label.ignore_word"),
                        contentX, ignoreWordField.getY() - this.font.lineHeight - 2, 0xFFAAAAAA, true);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);

        if (currentTab == Tab.BASIC && usageButton != null) {
            usageButton.setMessage(getUsageButtonText());
        }

        if (currentTab != Tab.BASIC && !listEntries.isEmpty()) {
            int btnX1 = contentX + contentWidth - 26;
            int btnX2 = contentX + contentWidth - 2;

            int y = listTop - scrollOffset;
            for (String listEntry : listEntries) {
                if (y + ENTRY_HEIGHT > listTop && y < listBottom) {
                    if (mouseX >= contentX && mouseX <= contentX + contentWidth
                            && mouseY >= y && mouseY <= y + ENTRY_HEIGHT) {
                        context.fill(contentX, y, contentX + contentWidth, y + ENTRY_HEIGHT, 0x44FFFFFF);
                    }

                    int textY = y + (ENTRY_HEIGHT - this.font.lineHeight) / 2;

                    context.text(
                            this.font,
                            Component.literal(listEntry),
                            contentX + 4,
                            textY,
                            0xFFFFFFFF,
                            true
                    );

                    context.fill(btnX1, y + 2, btnX2, y + ENTRY_HEIGHT - 2, 0xFF883333);

                    context.centeredText(
                            this.font,
                            Component.literal("x"),
                            (btnX1 + btnX2) / 2,
                            textY,
                            0xFFFFFFFF
                    );
                }
                y += ENTRY_HEIGHT;
            }

            int totalHeight   = listEntries.size() * ENTRY_HEIGHT;
            int visibleHeight = listBottom - listTop;
            if (totalHeight > visibleHeight) {
                int scrollbarHeight = Math.max(10, visibleHeight * visibleHeight / totalHeight);
                int scrollbarY      = listTop + scrollOffset * (visibleHeight - scrollbarHeight) / Math.max(1, totalHeight - visibleHeight);
                context.fill(contentX + contentWidth - 3, listTop,
                        contentX + contentWidth, listBottom, 0x44FFFFFF);
                context.fill(contentX + contentWidth - 3, scrollbarY,
                        contentX + contentWidth, scrollbarY + scrollbarHeight, 0xFFAAAAAA);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (currentTab != Tab.BASIC && !listEntries.isEmpty()) {
            int btnX1 = contentX + contentWidth - 26;
            int btnX2 = contentX + contentWidth - 2;
            int y = listTop - scrollOffset;
            for (int i = 0; i < listEntries.size(); i++) {
                if (y + ENTRY_HEIGHT > listTop && y < listBottom) {
                    if (click.x() >= btnX1 && click.x() <= btnX2
                            && click.y() >= y + 2 && click.y() <= y + ENTRY_HEIGHT - 2) {
                        listActions.get(i).run();
                        return true;
                    }
                }
                y += ENTRY_HEIGHT;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab != Tab.BASIC
                && mouseX >= contentX && mouseX <= contentX + contentWidth
                && mouseY >= listTop  && mouseY <= listBottom) {
            int totalHeight   = listEntries.size() * ENTRY_HEIGHT;
            int visibleHeight = listBottom - listTop;
            int maxScroll     = Math.max(0, totalHeight - visibleHeight);
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * ENTRY_HEIGHT));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private Component getHypixelButtonText() {
        return config.hypixelMode
                ? I18n.translate("translatormod.toggle.hypixel")
                : I18n.translate("translatormod.toggle.hypixel_off");
    }

    private Component getToggleText() {
        return config.enabled
                ? I18n.translate("translatormod.toggle.enabled_on")
                : I18n.translate("translatormod.toggle.enabled_off");
    }

    private Component getTooltipText() {
        return config.translateTooltips
                ? I18n.translate("translatormod.toggle.tooltip_on")
                : I18n.translate("translatormod.toggle.tooltip_off");
    }

    private Component getSystemMsgText() {
        return config.translateSystemMessages
                ? I18n.translate("translatormod.toggle.system_on")
                : I18n.translate("translatormod.toggle.system_off");
    }

    private Component getWaitMsgText() {
        return config.showWaitMessage
                ? I18n.translate("translatormod.toggle.waitmsg_on")
                : I18n.translate("translatormod.toggle.waitmsg_off");
    }

    private Component getUsageButtonText() {
        int remaining = TranslationHandler.getUsageCooldownRemaining();
        if (remaining > 0) {
            return I18n.translate("translatormod.button.usage_wait", remaining);
        }
        return I18n.translate("translatormod.button.usage");
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

