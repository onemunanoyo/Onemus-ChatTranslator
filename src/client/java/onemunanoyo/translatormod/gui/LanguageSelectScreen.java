package onemunanoyo.translatormod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.util.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class LanguageSelectScreen extends Screen {
    private final Screen parent;
    private final boolean isIncoming;
    private final Consumer<String> onSelect;

    private TextFieldWidget searchBox;
    private LanguageList list;
    private ButtonWidget doneButton;

    public static final List<LanguageInfo> LANGUAGES = List.of(
            new LanguageInfo("EN-US", "English (US)"),
            new LanguageInfo("EN-GB", "English (UK)"),
            new LanguageInfo("JA", "日本語 (Japanese)"),
            new LanguageInfo("ZH-HANS", "简体中文 (Chinese Simplified)"),
            new LanguageInfo("ZH-HANT", "繁體中文 (Traditional Chinese)"),
            new LanguageInfo("KO", "한국어 (Korean)"),
            new LanguageInfo("ES", "Español (Spanish)"),
            new LanguageInfo("FR", "Français (French)"),
            new LanguageInfo("DE", "Deutsch (German)"),
            new LanguageInfo("IT", "Italiano (Italian)"),
            new LanguageInfo("PT-BR", "Português (Brazilian Portuguese)"),
            new LanguageInfo("PT-PT", "Português (Portugal)"),
            new LanguageInfo("NL", "Nederlands (Dutch)"),
            new LanguageInfo("PL", "Polski (Polish)"),
            new LanguageInfo("RU", "Русский (Russian)"),
            new LanguageInfo("TR", "Türkçe (Turkish)"),
            new LanguageInfo("TH", "ไทย (Thai)"),
            new LanguageInfo("VI", "Tiếng Việt (Vietnamese)"),
            new LanguageInfo("ID", "Bahasa Indonesia (Indonesian)"),
            new LanguageInfo("AR", "العربية (Arabic)"),
            new LanguageInfo("BG", "Български (Bulgarian)"),
            new LanguageInfo("CS", "Čeština (Czech)"),
            new LanguageInfo("DA", "Dansk (Danish)"),
            new LanguageInfo("EL", "Ελληνικά (Greek)"),
            new LanguageInfo("ET", "Eesti (Estonian)"),
            new LanguageInfo("FI", "Suomi (Finnish)"),
            new LanguageInfo("HU", "Magyar (Hungarian)"),
            new LanguageInfo("LT", "Lietuvių (Lithuanian)"),
            new LanguageInfo("LV", "Latviešu (Latvian)"),
            new LanguageInfo("NB", "Norsk Bokmål (Norwegian)"),
            new LanguageInfo("RO", "Română (Romanian)"),
            new LanguageInfo("SK", "Slovenčina (Slovak)"),
            new LanguageInfo("SL", "Slovenščina (Slovenian)"),
            new LanguageInfo("SV", "Svenska (Swedish)"),
            new LanguageInfo("UK", "Українська (Ukrainian)")
    );

    public LanguageSelectScreen(Screen parent, boolean isIncoming, Consumer<String> onSelect) {
        super(isIncoming 
                ? I18n.translate("translatormod.gui.select_receive_lang") 
                : I18n.translate("translatormod.gui.select_send_lang"));
        this.parent = parent;
        this.isIncoming = isIncoming;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        // 検索ボックスの配置
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 22, 200, 20, Text.literal("Search"));
        this.searchBox.setChangedListener(this::filterLanguages);
        this.addDrawableChild(this.searchBox);

        // リストの配置
        int listTop = 48;
        int listHeight = this.height - listTop - 36;
        this.list = new LanguageList(this.client, this.width, listHeight, listTop, 24);
        this.addDrawableChild(this.list);

        // 下部ボタン
        this.doneButton = ButtonWidget.builder(Text.translatable("gui.done"), btn -> this.selectAndClose())
                .dimensions(this.width / 2 - 155, this.height - 28, 150, 20).build();
        this.doneButton.active = false;
        this.addDrawableChild(this.doneButton);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> this.close())
                .dimensions(this.width / 2 + 5, this.height - 28, 150, 20).build());

        this.filterLanguages("");
        
        TranslatorConfig config = TranslatorConfig.getInstance();
        String currentLang = isIncoming ? config.receiveTargetLang : config.trTargetLang;
        this.list.setSelectedByCode(currentLang);

        this.setInitialFocus(this.searchBox);
    }

    private void filterLanguages(String query) {
        this.list.filter(query);
        updateDoneButtonState();
    }

    private void updateDoneButtonState() {
        if (this.doneButton != null) {
            this.doneButton.active = this.list.getSelectedOrNull() != null;
        }
    }

    private void selectAndClose() {
        LanguageEntry selected = this.list.getSelectedOrNull();
        if (selected != null) {
            this.onSelect.accept(selected.info.code);
            this.close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public static String getLanguageName(String code) {
        if (code == null) return "Unknown";
        for (LanguageInfo info : LANGUAGES) {
            if (info.code.equalsIgnoreCase(code)) {
                return info.displayName;
            }
        }
        return code;
    }

    public static class LanguageInfo {
        public final String code;
        public final String displayName;

        public LanguageInfo(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
    }

    public class LanguageList extends AlwaysSelectedEntryListWidget<LanguageEntry> {
        private final List<LanguageEntry> allEntries = new ArrayList<>();

        public LanguageList(MinecraftClient client, int width, int height, int y0, int itemHeight) {
            super(client, width, height, y0, itemHeight);
            for (LanguageInfo info : LANGUAGES) {
                LanguageEntry entry = new LanguageEntry(info);
                this.allEntries.add(entry);
            }
        }

        public void filter(String query) {
            this.clearEntries();
            String q = query.toLowerCase(Locale.ROOT).trim();
            for (LanguageEntry entry : this.allEntries) {
                if (q.isEmpty() || entry.info.displayName.toLowerCase(Locale.ROOT).contains(q) 
                        || entry.info.code.toLowerCase(Locale.ROOT).contains(q)) {
                    this.addEntry(entry);
                }
            }
        }

        public void setSelectedByCode(String code) {
            if (code == null) return;
            String normalizedCode = code.toUpperCase().replace('_', '-');
            if (normalizedCode.startsWith("JA-") || normalizedCode.equals("JA")) normalizedCode = "JA";
            else if (normalizedCode.startsWith("KO-") || normalizedCode.equals("KO")) normalizedCode = "KO";
            else if (normalizedCode.equals("EN")) normalizedCode = "EN-US";
            else if (normalizedCode.equals("ZH")) normalizedCode = "ZH-HANS";
            
            for (LanguageEntry entry : this.allEntries) {
                if (entry.info.code.equalsIgnoreCase(normalizedCode)) {
                    this.setSelected(entry);
                    updateDoneButtonState();
                    break;
                }
            }
        }

        @Override
        public void setSelected(LanguageEntry entry) {
            super.setSelected(entry);
            updateDoneButtonState();
        }
    }

    public class LanguageEntry extends AlwaysSelectedEntryListWidget.Entry<LanguageEntry> {
        public final LanguageInfo info;

        public LanguageEntry(LanguageInfo info) {
            this.info = info;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getContentMiddleX();
            int y = this.getContentMiddleY() - LanguageSelectScreen.this.textRenderer.fontHeight / 2;
            context.drawCenteredTextWithShadow(LanguageSelectScreen.this.textRenderer, Text.literal(this.info.displayName), x, y, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
            LanguageSelectScreen.this.list.setSelected(this);
            return true;
        }

        @Override
        public Text getNarration() {
            return Text.literal(this.info.displayName);
        }
    }
}
