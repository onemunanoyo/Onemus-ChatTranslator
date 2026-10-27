package onemunanoyo.translatormod.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslatorConfigTest {

    @Test
    void correctsInvalidValuesAndRestoresSynchronizedCollections() throws Exception {
        Constructor<TranslatorConfig> constructor = TranslatorConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        TranslatorConfig config = constructor.newInstance();
        config.trTargetLang = "invalid";
        config.hypixelAllChat = "OFF";
        config.rules = null;
        config.ignoreWords = null;

        config.validate();

        assertEquals("EN-US", config.trTargetLang);
        assertEquals("CLICK", config.hypixelAllChat);
        assertNotNull(config.rules);
        assertNotNull(config.ignoreWords);
        assertTrue(config.rules.getClass().getName().contains("Synchronized"));
        assertTrue(config.ignoreWords.getClass().getName().contains("Synchronized"));
    }

    @Test
    void preservesValidValuesWhenValidating() throws Exception {
        Constructor<TranslatorConfig> constructor = TranslatorConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        TranslatorConfig config = constructor.newInstance();
        config.trTargetLang = "EN-GB";
        config.hypixelAllChat = "AUTO";
        config.rules = new LinkedHashMap<>();
        config.rules.put("EN", "JA");
        config.ignoreWords = new ArrayList<>();
        config.ignoreWords.add("keep");

        config.validate();

        assertEquals("EN-GB", config.trTargetLang);
        assertEquals("AUTO", config.hypixelAllChat);
        assertEquals("JA", config.rules.get("EN"));
        assertEquals("keep", config.ignoreWords.get(0));
    }
}
