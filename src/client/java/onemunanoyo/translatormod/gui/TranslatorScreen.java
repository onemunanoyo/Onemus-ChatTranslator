package onemunanoyo.translatormod.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import onemunanoyo.translatormod.TranslationHandler;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.util.DebugLog;
import onemunanoyo.translatormod.util.I18n;

import java.util.ArrayList;
import java.util.List;

public class TranslatorScreen extends Screen {

    private final Screen parent;
    private TranslatorConfig config;

    private enum Tab { BASIC, RULES, IGNORE, LANGUAGE }
    private Tab currentTab = Tab.BASIC;

    private TextFieldWidget apiKeyField;
    private TextFieldWidget ignoreWordField;
    private ButtonWidget trLangButton;
    private ButtonWidget receiveLangButton;

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
    private ButtonWidget usageButton = null;

    private Text translatorTitle = Text.empty();

    public TranslatorScreen(Screen parent) {
        super(Text.empty());
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

        this.addDrawableChild(ButtonWidget.builder(
                currentTab == Tab.BASIC ? I18n.translate("translatormod.tab.basic").copy().formatted(Formatting.YELLOW) : I18n.translate("translatormod.tab.basic"),
                btn -> { currentTab = Tab.BASIC; scrollOffset = 0; clearAndInit(); }
        ).dimensions(contentX, tabY, tabWidth - 2, TAB_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                currentTab == Tab.RULES ? I18n.translate("translatormod.tab.rules").copy().formatted(Formatting.YELLOW) : I18n.translate("translatormod.tab.rules"),
                btn -> { currentTab = Tab.RULES; scrollOffset = 0; clearAndInit(); }
        ).dimensions(contentX + tabWidth, tabY, tabWidth - 2, TAB_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                currentTab == Tab.IGNORE ? I18n.translate("translatormod.tab.ignore").copy().formatted(Formatting.YELLOW) : I18n.translate("translatormod.tab.ignore"),
                btn -> { currentTab = Tab.IGNORE; scrollOffset = 0; clearAndInit(); }
        ).dimensions(contentX + tabWidth * 2, tabY, tabWidth - 2, TAB_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                currentTab == Tab.LANGUAGE ? I18n.translate("translatormod.tab.language").copy().formatted(Formatting.YELLOW) : I18n.translate("translatormod.tab.language"),
                btn -> { currentTab = Tab.LANGUAGE; scrollOffset = 0; clearAndInit(); }
        ).dimensions(contentX + tabWidth * 3, tabY, tabWidth - 2, TAB_HEIGHT).build());

        int contentY = tabY + TAB_HEIGHT + PADDING + this.textRenderer.fontHeight + 4;

        switch (currentTab) {
            case BASIC    -> initBasicTab(contentX, contentY, contentWidth);
            case RULES    -> initRulesTab(contentX, contentY, contentWidth);
            case IGNORE   -> initIgnoreTab(contentX, contentY, contentWidth);
            case LANGUAGE -> initLanguageTab(contentX, contentY, contentWidth);
        }

        int closeY = panelY + panelHeight - BUTTON_HEIGHT - PADDING;
        this.addDrawableChild(ButtonWidget.builder(
                I18n.translate("translatormod.button.close"),
                btn -> this.close()
        ).dimensions(contentX, closeY, contentWidth, BUTTON_HEIGHT).build());
    }

    private void initBasicTab(int x, int y, int w) {
        listEntries.clear();
        listActions.clear();

        this.addDrawableChild(ButtonWidget.builder(
                getToggleText(),
                btn -> {
                    if (!config.enabled && config.apiKey.isBlank()) {
                        if (client != null && client.player != null)
                            client.player.sendMessage(I18n.translate("translatormod.command.on.no_api"), true);
                        return;
                    }
                    config.enabled = !config.enabled;
                    config.save();
                    btn.setMessage(getToggleText());
                }
        ).dimensions(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        y += this.textRenderer.fontHeight + 4;

        apiKeyField = new TextFieldWidget(
                this.textRenderer, x, y, w - 82, FIELD_HEIGHT, I18n.translate("translatormod.label.api_key")
        );
        apiKeyField.setMaxLength(256);
        apiKeyField.setPlaceholder(I18n.translate(
                config.apiKey.isBlank() ? "translatormod.placeholder.api_key_empty" : "translatormod.placeholder.api_key_filled",
                config.apiKey.substring(Math.max(0, config.apiKey.length() - 4))
        ));
        this.addDrawableChild(apiKeyField);

        ButtonWidget saveButton = ButtonWidget.builder(
                I18n.translate("translatormod.button.save"),
                btn -> {
                    String input = apiKeyField.getText().trim();
                    int cooldown = TranslationHandler.getValidationCooldownRemaining();
                    if (!shouldStartApiKeyValidation(input, cooldown)) {
                        if (cooldown > 0 && client != null && client.player != null) {
                            client.player.sendMessage(I18n.translate("translatormod.gui.api_key.cooldown", cooldown), true);
                        }
                        return;
                    }

                    btn.active = false;
                    btn.setMessage(I18n.translate("translatormod.gui.api_key.verifying"));

                    TranslationHandler.validateApiKeyAsync(input).thenAccept(success -> {
                        if (client != null) {
                            client.execute(() -> {
                                btn.active = true;
                                btn.setMessage(I18n.translate("translatormod.button.save"));

                                if (shouldApplyValidatedApiKey(success)) {
                                    if (config.setApiKey(input)) {
                                        TranslationHandler.clearCache();
                                        apiKeyField.setText("");
                                        apiKeyField.setPlaceholder(I18n.translate("translatormod.placeholder.api_key_filled",
                                                config.apiKey.substring(Math.max(0, config.apiKey.length() - 4))
                                        ));
                                        if (client.player != null) {
                                            client.player.sendMessage(I18n.translate("translatormod.gui.api_key.verified"), true);
                                        }
                                    }
                                } else if (client.player != null) {
                                    client.player.sendMessage(I18n.translate("translatormod.gui.api_key.invalid"), true);
                                }
                            });
                        }
                    });
                }
        ).dimensions(x + w - 80, y, 80, BUTTON_HEIGHT).build();
        this.addDrawableChild(saveButton);
        y += FIELD_HEIGHT + PADDING;

        this.addDrawableChild(ButtonWidget.builder(
                getTooltipText(),
                btn -> {
                    config.translateTooltips = !config.translateTooltips;
                    config.save();
                    btn.setMessage(getTooltipText());
                }
        ).dimensions(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        this.addDrawableChild(ButtonWidget.builder(
                getSystemMsgText(),
                btn -> {
                    config.translateSystemMessages = !config.translateSystemMessages;
                    config.save();
                    btn.setMessage(getSystemMsgText());
                }
        ).dimensions(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        int halfW = w / 2 - 2;
        this.addDrawableChild(ButtonWidget.builder(
                I18n.translate("translatormod.button.cache_clear"),
                btn -> TranslationHandler.clearCache()
        ).dimensions(x, y, halfW, BUTTON_HEIGHT).build());

        usageButton = ButtonWidget.builder(
                getUsageButtonText(),
                btn -> {
                    if (TranslationHandler.getUsageCooldownRemaining() > 0) return;
                    btn.setMessage(I18n.translate("translatormod.button.usage_loading"));
                    TranslationHandler.fetchUsageAsync().thenAccept(result -> {
                        DebugLog.log("Usage result received: " + result);
                        if (client != null) {
                            client.execute(() -> {
                                if (client.player != null) {
                                    DebugLog.log("Sending message to player: " + client.player.getName().getString());
                                    client.player.sendMessage(buildUsageChatMessage(result), false);
                                } else {
                                    DebugLog.log("Player is null! client=true");
                                }
                                if (usageButton != null) usageButton.setMessage(getUsageButtonText());
                            });
                        } else {
                            DebugLog.log("Client is null!");
                        }
                    });
                }
        ).dimensions(x + halfW + 4, y, halfW, BUTTON_HEIGHT).build();
        this.addDrawableChild(usageButton);
        y += BUTTON_HEIGHT + PADDING;
    }

    private void initLanguageTab(int x, int y, int w) {
        this.addDrawableChild(ButtonWidget.builder(
                getLanguageButtonText(),
                btn -> {
                    config.uiLanguage = nextUiLanguage(config.uiLanguage);
                    config.save();
                    refreshTitle();
                    clearAndInit(); // 言語切り替え時に画面再描画
                }
        ).dimensions(x, y, w, BUTTON_HEIGHT).build());
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

    private Text getLanguageButtonText() {
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


    private Text buildUsageChatMessage(String result) {
        String prefix = I18n.getString("translatormod.msg.prefix");
        String errorPrefix = I18n.getString("translatormod.usage.error_prefix");
        String noApiMsg = I18n.getString("translatormod.usage.error.no_api");

        // result自体が翻訳キー（未翻訳状態）である可能性も考慮し、翻訳後の文字列でも比較する
        boolean isError = result.startsWith(errorPrefix) || result.equals(noApiMsg);

        if (isError) {
            return Text.literal(prefix + " " + result)
                    .formatted(Formatting.RED);
        }
        try {
            int pctStart = result.lastIndexOf('(') + 1;
            int pctEnd   = result.lastIndexOf('%');
            if (pctStart > 0 && pctEnd > pctStart) {
                String pctStr = result.substring(pctStart, pctEnd).trim().replace(",", ".");
                double pct = Double.parseDouble(pctStr);
                Formatting color = pct >= 95.0 ? Formatting.RED
                        : pct >= 80.0 ? Formatting.YELLOW
                        : Formatting.GREEN;
                return Text.literal(prefix + " " + result).formatted(color);
            }
        } catch (NumberFormatException ignored) {}
        return Text.literal(prefix + " " + result).formatted(Formatting.GRAY);
    }

    private void initRulesTab(int x, int y, int w) {
        y += this.textRenderer.fontHeight + 4;

        receiveLangButton = ButtonWidget.builder(
                Text.literal(I18n.getString("translatormod.label.receive_lang") + " " + LanguageSelectScreen.getLanguageName(config.receiveTargetLang)),
                btn -> {
                    if (this.client != null) {
                        this.client.setScreen(new LanguageSelectScreen(this, true, selectedLang -> {
                            config.receiveTargetLang = selectedLang;
                            config.save();
                            TranslationHandler.clearCache();
                        }));
                    }
                }
        ).dimensions(x, y, w, BUTTON_HEIGHT).build();
        this.addDrawableChild(receiveLangButton);
        y += BUTTON_HEIGHT + PADDING * 2;

        y += this.textRenderer.fontHeight + 4;

        trLangButton = ButtonWidget.builder(
                Text.literal(I18n.getString("translatormod.label.send_lang") + " " + LanguageSelectScreen.getLanguageName(config.trTargetLang)),
                btn -> {
                    if (this.client != null) {
                        this.client.setScreen(new LanguageSelectScreen(this, false, selectedLang -> {
                            config.trTargetLang = selectedLang;
                            config.save();
                        }));
                    }
                }
        ).dimensions(x, y, w, BUTTON_HEIGHT).build();
        this.addDrawableChild(trLangButton);
        y += BUTTON_HEIGHT + PADDING * 2;

        this.addDrawableChild(ButtonWidget.builder(
                getHypixelButtonText(),
                btn -> {
                    if (this.client != null) {
                        this.client.setScreen(new HypixelFilterScreen(this));
                    }
                }
        ).dimensions(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        this.addDrawableChild(ButtonWidget.builder(
                getWaitMsgText(),
                btn -> {
                    config.showWaitMessage = !config.showWaitMessage;
                    config.save();
                    btn.setMessage(getWaitMsgText());
                }
        ).dimensions(x, y, w, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + PADDING;

        listEntries.clear();
        listActions.clear();
    }

    private void initIgnoreTab(int x, int y, int w) {
        ignoreWordField = new TextFieldWidget(
                this.textRenderer, x, y, w - 52, FIELD_HEIGHT, Text.literal("ignoreWord")
        );
        ignoreWordField.setMaxLength(64);
        ignoreWordField.setPlaceholder(I18n.translate("translatormod.placeholder.ignore_word"));
        this.addDrawableChild(ignoreWordField);

        this.addDrawableChild(ButtonWidget.builder(
                I18n.translate("translatormod.button.add"),
                btn -> {
                    String word = ignoreWordField.getText().trim();
                    if (!word.isEmpty()) {
                        config.addIgnoreWord(word);
                        ignoreWordField.setText("");
                        clearAndInit();
                    }
                }
        ).dimensions(x + w - 48, y, 48, FIELD_HEIGHT).build());
        y += FIELD_HEIGHT + PADDING;

        listTop = y;
        listBottom = panelY + panelHeight - FOOTER_HEIGHT - PADDING;
        listEntries.clear();
        listActions.clear();
        for (String word : new ArrayList<>(config.ignoreWords)) {
            listEntries.add(word);
            listActions.add(() -> { config.removeIgnoreWord(word); clearAndInit(); });
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF555555);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF555555);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
        context.drawCenteredTextWithShadow(this.textRenderer, this.translatorTitle, this.width / 2, panelY + PADDING, 0xFFFFFFFF);

        if (currentTab == Tab.BASIC) {
            if (apiKeyField != null)
                context.drawTextWithShadow(this.textRenderer, I18n.translate("translatormod.label.api_key"),
                        contentX, apiKeyField.getY() - this.textRenderer.fontHeight - 2, 0xFFAAAAAA);
        } else if (currentTab == Tab.IGNORE) {
            if (ignoreWordField != null)
                context.drawTextWithShadow(this.textRenderer, I18n.translate("translatormod.label.ignore_word"),
                        contentX, ignoreWordField.getY() - this.textRenderer.fontHeight - 2, 0xFFAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);

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

                    int textY = y + (ENTRY_HEIGHT - this.textRenderer.fontHeight) / 2;

                    context.drawTextWithShadow(
                            this.textRenderer,
                            Text.literal(listEntry),
                            contentX + 4,
                            textY,
                            0xFFFFFFFF
                    );

                    context.fill(btnX1, y + 2, btnX2, y + ENTRY_HEIGHT - 2, 0xFF883333);

                    context.drawCenteredTextWithShadow(
                            this.textRenderer,
                            Text.literal("x"),
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
    public boolean mouseClicked(Click click, boolean doubled) {
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

    private Text getHypixelButtonText() {
        return config.hypixelMode
                ? I18n.translate("translatormod.toggle.hypixel")
                : I18n.translate("translatormod.toggle.hypixel_off");
    }

    private Text getToggleText() {
        return config.enabled
                ? I18n.translate("translatormod.toggle.enabled_on")
                : I18n.translate("translatormod.toggle.enabled_off");
    }

    private Text getTooltipText() {
        return config.translateTooltips
                ? I18n.translate("translatormod.toggle.tooltip_on")
                : I18n.translate("translatormod.toggle.tooltip_off");
    }

    private Text getSystemMsgText() {
        return config.translateSystemMessages
                ? I18n.translate("translatormod.toggle.system_on")
                : I18n.translate("translatormod.toggle.system_off");
    }

    private Text getWaitMsgText() {
        return config.showWaitMessage
                ? I18n.translate("translatormod.toggle.waitmsg_on")
                : I18n.translate("translatormod.toggle.waitmsg_off");
    }

    private Text getUsageButtonText() {
        int remaining = TranslationHandler.getUsageCooldownRemaining();
        if (remaining > 0) {
            return I18n.translate("translatormod.button.usage_wait", remaining);
        }
        return I18n.translate("translatormod.button.usage");
    }

    static boolean shouldStartApiKeyValidation(String input, int cooldownRemaining) {
        return input != null && !input.isBlank() && cooldownRemaining <= 0;
    }

    static boolean shouldApplyValidatedApiKey(Boolean validationResult) {
        return Boolean.TRUE.equals(validationResult);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
