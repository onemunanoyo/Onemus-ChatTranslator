package onemunanoyo.translatormod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHudMixinTest {

    @Test
    void excludesApiKeyCommandsFromRecentChatHistory() {
        assertTrue(TranslatorModClient.shouldSkipRecentChat("/translate key secret"));
        assertTrue(TranslatorModClient.shouldSkipRecentChat("  /translate key secret"));
        assertFalse(TranslatorModClient.shouldSkipRecentChat("/translate status"));
        assertFalse(TranslatorModClient.shouldSkipRecentChat("/tr hello"));
        assertFalse(TranslatorModClient.shouldSkipRecentChat("hello"));
        assertFalse(TranslatorModClient.shouldSkipRecentChat(null));
    }

    @Test
    void buildsSafeTranslateCommand() {
        assertEquals("/tr \"a\\\"b\\\\c\"",
                TranslatorModClient.buildTranslateCommand("a\"b\\c\n\t"));
        assertEquals("/tr \"\"", TranslatorModClient.buildTranslateCommand(null));
        assertEquals("/tr \"ab\"",
                TranslatorModClient.buildTranslateCommand("a\u0000\u001f\u007fb"));
    }

    @Test
    void truncatesTranslateCommandInputToTwoHundredCharacters() {
        String command = TranslatorModClient.buildTranslateCommand("x".repeat(201));

        assertEquals("/tr \"" + "x".repeat(200) + "\"", command);
    }
}
