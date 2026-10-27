package onemunanoyo.translatormod.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslatorScreenTest {

    @Test
    void apiKeyValidationRequiresNonBlankInputAndNoCooldown() {
        assertTrue(TranslatorScreen.shouldStartApiKeyValidation(" secret ", 0));
        assertFalse(TranslatorScreen.shouldStartApiKeyValidation(null, 0));
        assertFalse(TranslatorScreen.shouldStartApiKeyValidation("   ", 0));
        assertFalse(TranslatorScreen.shouldStartApiKeyValidation("secret", 1));
    }

    @Test
    void onlySuccessfulValidationMayApplyTheApiKey() {
        assertTrue(TranslatorScreen.shouldApplyValidatedApiKey(Boolean.TRUE));
        assertFalse(TranslatorScreen.shouldApplyValidatedApiKey(Boolean.FALSE));
        assertFalse(TranslatorScreen.shouldApplyValidatedApiKey(null));
    }

    @Test
    void languageListIncludesTheAdditionalRegionalLanguages() {
        assertTrue(LanguageSelectScreen.getLanguageName("EN-GB").contains("English (UK)"));
        assertTrue(LanguageSelectScreen.getLanguageName("ZH-HANT").contains("Traditional Chinese"));
        assertTrue(LanguageSelectScreen.getLanguageName("PT-PT").contains("Português (Portugal)"));
    }
}
