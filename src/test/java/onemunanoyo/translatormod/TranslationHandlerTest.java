package onemunanoyo.translatormod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranslationHandlerTest {

    @Test
    void normalizesDeepLLanguageCodes() {
        assertEquals("EN-US", TranslationHandler.normalizeLanguageCode(" en "));
        assertEquals("PT-BR", TranslationHandler.normalizeLanguageCode("pt"));
        assertEquals("ZH-HANS", TranslationHandler.normalizeLanguageCode("ZH"));
        assertEquals("JA", TranslationHandler.normalizeLanguageCode("JA_JP"));
        assertEquals("EN-GB", TranslationHandler.normalizeLanguageCode("en-gb"));
        assertEquals("PT-PT", TranslationHandler.normalizeLanguageCode(" PT_PT "));
        assertEquals("ZH-HANT", TranslationHandler.normalizeLanguageCode("zh_hant"));
    }

    @Test
    void parsesValidTranslationResponse() throws Exception {
        assertArrayEquals(
                new String[]{"こんにちは", "EN"},
                TranslationHandler.parseTranslationResponse(
                        "{\"translations\":[{\"text\":\"こんにちは\",\"detected_source_language\":\"EN\"}]}"));
    }

    @Test
    void rejectsInvalidTranslationResponses() {
        assertThrows(TranslationHandler.ParseException.class,
                () -> TranslationHandler.parseTranslationResponse("not-json"));
        assertThrows(TranslationHandler.ParseException.class,
                () -> TranslationHandler.parseTranslationResponse("{}"));
        assertThrows(TranslationHandler.ParseException.class,
                () -> TranslationHandler.parseTranslationResponse("{\"translations\":[]}"));
        assertThrows(TranslationHandler.ParseException.class,
                () -> TranslationHandler.parseTranslationResponse(
                        "{\"translations\":[{\"text\":\"x\"}]}"));
        assertThrows(TranslationHandler.ParseException.class,
                () -> TranslationHandler.parseTranslationResponse(
                        "{\"translations\":[{\"detected_source_language\":\"EN\"}]}"));
    }

    @Test
    void parsesAndValidatesUsageResponse() throws Exception {
        assertArrayEquals(new long[]{12L, 500_000L},
                TranslationHandler.parseUsageResponse(
                        "{\"character_count\":12,\"character_limit\":500000}"));
        assertThrows(TranslationHandler.ParseException.class,
                () -> TranslationHandler.parseUsageResponse("{}"));
        assertThrows(TranslationHandler.ParseException.class,
                () -> TranslationHandler.parseUsageResponse("not-json"));
    }
}
