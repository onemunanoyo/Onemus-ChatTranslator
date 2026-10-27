package onemunanoyo.translatormod.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.text.Text;
import onemunanoyo.translatormod.config.TranslatorConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class I18n {
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    public static Text translate(String key, Object... args) {
        String lang = TranslatorConfig.getInstance().uiLanguage;
        if ("AUTO".equals(lang)) {
            return Text.translatable(key, args);
        }

        Map<String, String> translations = getTranslations(lang.toLowerCase());
        String template = translations.getOrDefault(key, key);
        try {
            return Text.literal(String.format(template, args));
        } catch (Exception e) {
            System.err.println("[TranslatorMod] I18n format error for key " + key + ": " + e.getMessage());
            return Text.literal(template);
        }
    }

    private static Map<String, String> getTranslations(String lang) {
        return CACHE.computeIfAbsent(lang, l -> {
            String path = "/assets/translatormod/lang/" + l + ".json";
            try (InputStream is = I18n.class.getResourceAsStream(path)) {
                if (is != null) {
                    return GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8),
                            new TypeToken<Map<String, String>>() {}.getType());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new ConcurrentHashMap<>();
        });
    }

    public static String getString(String key, Object... args) {
        return translate(key, args).getString();
    }
}
