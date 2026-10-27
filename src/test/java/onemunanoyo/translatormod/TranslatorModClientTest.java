package onemunanoyo.translatormod;

import onemunanoyo.translatormod.config.TranslatorConfig;
import onemunanoyo.translatormod.util.I18n;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslatorModClientTest {

    @Test
    void recognizesOnlyPlayerNamesBeforeAColonAsAllChat() {
        TranslatorConfig config = TranslatorConfig.getInstance();
        String previousAllChat = config.hypixelAllChat;
        try {
            config.hypixelAllChat = "CLICK";

            assertEquals("CLICK", TranslatorModClient.getHypixelChannelMode("Player123: hello"));
            assertEquals("CLICK", TranslatorModClient.getHypixelChannelMode("abc: hello"));
            assertEquals("OFF", TranslatorModClient.getHypixelChannelMode("ab: hello"));
            assertEquals("OFF", TranslatorModClient.getHypixelChannelMode("Player-123: hello"));
            assertEquals("OFF", TranslatorModClient.getHypixelChannelMode("Game starts in: 5 seconds"));
            assertEquals("CLICK", TranslatorModClient.getHypixelChannelMode("<Player123> hello"));
            assertEquals("CLICK", TranslatorModClient.getHypixelChannelMode("§xPlayer123: hello"));
        } finally {
            config.hypixelAllChat = previousAllChat;
        }
    }

    @Test
    void preservesPartyGuildAndDirectMessageChannelModes() {
        TranslatorConfig config = TranslatorConfig.getInstance();
        String previousParty = config.hypixelParty;
        String previousGuild = config.hypixelGuild;
        String previousDm = config.hypixelDM;
        try {
            config.hypixelParty = "AUTO";
            config.hypixelGuild = "CLICK";
            config.hypixelDM = "OFF";

            assertEquals("AUTO", TranslatorModClient.getHypixelChannelMode("Party > hello"));
            assertEquals("CLICK", TranslatorModClient.getHypixelChannelMode("Guild > hello"));
            assertEquals("OFF", TranslatorModClient.getHypixelChannelMode("From Player123: hello"));
            assertEquals("OFF", TranslatorModClient.getHypixelChannelMode("To Player123: hello"));
        } finally {
            config.hypixelParty = previousParty;
            config.hypixelGuild = previousGuild;
            config.hypixelDM = previousDm;
        }
    }

    @Test
    void returnsTheUnformattedTemplateAfterLoggingFormatFailures() {
        TranslatorConfig config = TranslatorConfig.getInstance();
        String previousLanguage = config.uiLanguage;
        try {
            config.uiLanguage = "EN_US";
            assertEquals("Check Usage (Wait %ds)",
                    I18n.getString("translatormod.button.usage_wait", "not-a-number"));
        } finally {
            config.uiLanguage = previousLanguage;
        }
    }
}
