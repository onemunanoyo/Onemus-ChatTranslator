package onemunanoyo.translatormod.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageSelectScreenTest {

    @Test
    void filtersByCodeAndDisplayName() {
        LanguageSelectScreen.LanguageInfo japanese =
                new LanguageSelectScreen.LanguageInfo("JA", "日本語 (Japanese)");

        assertTrue(LanguageSelectScreen.matchesQuery(japanese, "ja"));
        assertTrue(LanguageSelectScreen.matchesQuery(japanese, "japanese"));
        assertTrue(LanguageSelectScreen.matchesQuery(japanese, "  "));
        assertFalse(LanguageSelectScreen.matchesQuery(japanese, "english"));
    }

    @Test
    void normalizesMinecraftLocaleCodesForSelection() {
        assertEquals("EN-US", LanguageSelectScreen.normalizeSelectionCode(" en_us "));
        assertEquals("PT-BR", LanguageSelectScreen.normalizeSelectionCode("PT"));
        assertEquals("ZH-HANS", LanguageSelectScreen.normalizeSelectionCode("zh_hans"));
        assertEquals("JA", LanguageSelectScreen.normalizeSelectionCode("JA_JP"));
        assertEquals("PT-PT", LanguageSelectScreen.normalizeSelectionCode("pt_pt"));
    }
}
