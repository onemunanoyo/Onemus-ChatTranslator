package onemunanoyo.translatormod.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslateCommandTest {

    @Test
    void masksApiKeysWithOnlyTheirLastFourCharactersVisible() {
        assertEquals("****", TranslateCommand.maskKey("1234"));
        assertEquals("****5678", TranslateCommand.maskKey("12345678"));
    }

    @Test
    void buildsTheTranslateButtonCommandWithEscapingAndTruncation() {
        assertEquals("/tr \"say \\\"hello\\\" \\\\ path\"",
                TranslateCommand.buildTranslateCommand("say \"hello\" \\ path"));
        assertEquals("/tr \"linetab\"", TranslateCommand.buildTranslateCommand("line\ntab\t"));
        assertEquals("/tr \"" + "a".repeat(200) + "\"",
                TranslateCommand.buildTranslateCommand("a".repeat(201)));
        assertEquals("/tr \"\"", TranslateCommand.buildTranslateCommand(null));
    }

    @Test
    void skipsOnlyApiKeyCommandsFromChatHistory() {
        assertTrue(TranslateCommand.shouldSkipMessageHistory("/translate key secret"));
        assertTrue(TranslateCommand.shouldSkipMessageHistory("  /translate key secret"));
        assertFalse(TranslateCommand.shouldSkipMessageHistory("/translate status"));
        assertFalse(TranslateCommand.shouldSkipMessageHistory("/tr hello"));
        assertFalse(TranslateCommand.shouldSkipMessageHistory("hello"));
        assertFalse(TranslateCommand.shouldSkipMessageHistory(null));
    }
}
