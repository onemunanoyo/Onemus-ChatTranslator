package onemunanoyo.translatormod;

import onemunanoyo.translatormod.config.TranslatorConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslatorModClientTest {

    @Test
    void recognizesOnlyPlayerLikePrefixesAsAllChat() {
        assertTrue(TranslatorModClient.isAllChatMessage("Player123: hello"));
        assertTrue(TranslatorModClient.isAllChatMessage("abc: hello"));
        assertFalse(TranslatorModClient.isAllChatMessage("ab: hello"));
        assertFalse(TranslatorModClient.isAllChatMessage("Player-123: hello"));
        assertFalse(TranslatorModClient.isAllChatMessage("Game starts in: 5 seconds"));
        assertTrue(TranslatorModClient.isAllChatMessage("<Player123> hello"));
    }

    @Test
    void preservesPartyGuildAndDirectMessageChannelDetection() throws Exception {
        Constructor<TranslatorConfig> constructor = TranslatorConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        TranslatorConfig config = constructor.newInstance();
        config.hypixelParty = "PARTY_TEST";
        config.hypixelGuild = "GUILD_TEST";
        config.hypixelDM = "DM_TEST";

        Field instance = TranslatorConfig.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        instance.set(null, config);
        try {
            assertEquals("PARTY_TEST", TranslatorModClient.getHypixelChannelMode("Party > hello"));
            assertEquals("GUILD_TEST", TranslatorModClient.getHypixelChannelMode("Guild > hello"));
            assertEquals("DM_TEST", TranslatorModClient.getHypixelChannelMode("From Player123"));
            assertEquals("DM_TEST", TranslatorModClient.getHypixelChannelMode("To Player123"));
        } finally {
            instance.set(null, previous);
        }
    }
}
