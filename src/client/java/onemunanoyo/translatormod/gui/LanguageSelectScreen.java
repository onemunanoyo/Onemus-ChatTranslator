package onemunanoyo.translatormod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
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

    private LanguageList list;
    private EditBox searchBox;
    private Button doneButton;

    private static final List<LanguageInfo> LANGUAGES = List.of(
            new LanguageInfo("JA", "日本語 (Japanese)"),
            new LanguageInfo("EN-US", "English (US)"),
            new LanguageInfo("EN-GB", "English (UK)"),
            new LanguageInfo("ZH-HANS", "简体中文 (Simplified Chinese)"),
            new LanguageInfo("ZH-HANT", "繁體中文 (Traditional Chinese)"),
            new LanguageInfo("KO", "한국어 (Korean)"),
            new LanguageInfo("FR", "Français (French)"),
            new LanguageInfo("DE", "Deutsch (German)"),
            new LanguageInfo("ES", "Español (Spanish)"),
            new LanguageInfo("PT-BR", "Português (Brasil)"),
            new LanguageInfo("PT-PT", "Português (Portugal)"),
            new LanguageInfo("IT", "Italiano (Italian)"),
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
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, Component.literal("Search"));
        this.searchBox.setResponder(this::filterLanguages);
        this.addRenderableWidget(this.searchBox);

        int listTop = 48;
        int listHeight = this.height - listTop - 36;
        this.list = new LanguageList(this.minecraft, this.width, listHeight, listTop, 24);
        this.addRenderableWidget(this.list);

        this.doneButton = Button.builder(
                Component.translatable("gui.done"),
                btn -> this.selectAndClose()
        ).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build();
        this.doneButton.active = false;
        this.addRenderableWidget(this.doneButton);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                btn -> this.onClose()
        ).bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());

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
            this.doneButton.active = this.list.getSelected() != null;
        }
    }

    private void selectAndClose() {
        LanguageEntry selected = this.list.getSelected();
        if (selected != null) {
            this.onSelect.accept(selected.info.code);
            this.onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
        context.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    public static String getLanguageName(String code) {
        if (code == null) return "Unknown";
        String normalizedCode = normalizeSelectionCode(code);
        for (LanguageInfo info : LANGUAGES) {
            if (info.code.equalsIgnoreCase(normalizedCode)) {
                return info.displayName;
            }
        }
        return code;
    }

    static boolean matchesQuery(LanguageInfo info, String query) {
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        return normalizedQuery.isEmpty()
                || info.displayName.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || info.code.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    static String normalizeSelectionCode(String code) {
        if (code == null) return null;
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT).replace('_', '-');
        return switch (normalizedCode) {
            case "EN" -> "EN-US";
            case "PT" -> "PT-BR";
            case "ZH" -> "ZH-HANS";
            case "JA-JP" -> "JA";
            case "KO-KR" -> "KO";
            case "FR-FR" -> "FR";
            case "DE-DE" -> "DE";
            case "ES-ES" -> "ES";
            case "IT-IT" -> "IT";
            case "NL-NL" -> "NL";
            case "PL-PL" -> "PL";
            case "RU-RU" -> "RU";
            case "TR-TR" -> "TR";
            case "TH-TH" -> "TH";
            case "VI-VN" -> "VI";
            case "ID-ID" -> "ID";
            default -> normalizedCode;
        };
    }

    public static class LanguageInfo {
        public final String code;
        public final String displayName;

        public LanguageInfo(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
    }

    public class LanguageList extends ObjectSelectionList<LanguageEntry> {
        private final List<LanguageEntry> allEntries = new ArrayList<>();

        public LanguageList(Minecraft minecraft, int width, int height, int y0, int itemHeight) {
            super(minecraft, width, height, y0, itemHeight);
            for (LanguageInfo info : LANGUAGES) {
                this.allEntries.add(new LanguageEntry(info));
            }
        }

        public void filter(String query) {
            this.clearEntries();
            for (LanguageEntry entry : this.allEntries) {
                if (matchesQuery(entry.info, query)) {
                    this.addEntry(entry);
                }
            }
        }

        public void setSelectedByCode(String code) {
            String normalizedCode = normalizeSelectionCode(code);
            if (normalizedCode == null) return;
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

    public class LanguageEntry extends ObjectSelectionList.Entry<LanguageEntry> {
        public final LanguageInfo info;

        public LanguageEntry(LanguageInfo info) {
            this.info = info;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentXMiddle();
            int y = this.getContentY() + (this.getHeight() - LanguageSelectScreen.this.font.lineHeight) / 2;
            graphics.centeredText(LanguageSelectScreen.this.font, Component.literal(this.info.displayName), x, y, 0xFFFFFFFF);
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.info.displayName);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
            LanguageSelectScreen.this.list.setSelected(this);
            return true;
        }
    }
}
