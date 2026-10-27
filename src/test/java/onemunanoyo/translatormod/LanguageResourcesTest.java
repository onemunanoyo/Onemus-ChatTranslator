package onemunanoyo.translatormod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageResourcesTest {

    private static final List<String> LANGUAGES = List.of(
            "de_de", "en_us", "es_es", "fr_fr", "ja_jp",
            "ko_kr", "pt_br", "th_th", "vi_vn", "zh_hans"
    );

    private static final List<String> REQUIRED_KEYS = List.of(
            "translatormod.chat.error.parse",
            "translatormod.usage.error.parse",
            "translatormod.gui.select_receive_lang",
            "translatormod.gui.select_send_lang",
            "translatormod.gui.api_key.verifying",
            "translatormod.gui.api_key.verified",
            "translatormod.gui.api_key.invalid",
            "translatormod.gui.api_key.cooldown"
    );

    @Test
    void everySupportedLanguageContainsNewRuntimeMessages() {
        for (String language : LANGUAGES) {
            try (InputStream stream = getClass().getResourceAsStream(
                    "/assets/translatormod/lang/" + language + ".json")) {
                assertTrue(stream != null, "Missing language resource: " + language);
                JsonObject json = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : REQUIRED_KEYS) {
                    assertTrue(json.has(key), language + " is missing " + key);
                    assertTrue(!json.get(key).getAsString().isBlank(), language + " has a blank " + key);
                }
            } catch (Exception e) {
                throw new AssertionError("Could not read language resource: " + language, e);
            }
        }
    }

    @Test
    void englishHelpUsesTheBackportedCommandSemantics() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(
                "/assets/translatormod/lang/en_us.json")) {
            assertTrue(stream != null);
            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("§b/tr <text>§7: Translates text immediately (for buttons).",
                    json.get("translatormod.command.help.tr").getAsString());
            assertEquals("§b/trt <text>§7: Translates text and puts it in the chat box.",
                    json.get("translatormod.command.help.trt").getAsString());
        }
    }
}
