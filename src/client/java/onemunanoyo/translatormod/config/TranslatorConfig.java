package onemunanoyo.translatormod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import onemunanoyo.translatormod.config.encryption.ApiKeySecretStore;
import onemunanoyo.translatormod.config.encryption.EncryptionNotice;
import onemunanoyo.translatormod.config.encryption.SecretStoreResult;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TranslatorConfig {

    private static final Pattern LEGACY_API_KEY_FIELD = Pattern.compile(
            "(?s)\\\"apiKey\\\"\\s*:\\s*\\\"(?:\\\\.|[^\\\"\\\\])*\\\"\\s*,?");
    private static final Pattern MALFORMED_LEGACY_API_KEY_FIELD = Pattern.compile(
            "(?s)\\\"apiKey\\\"\\s*:.*?(?=\\R|$)");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("translatormod.json");
    private static final Path SECRET_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("translatormod.env");

    private static TranslatorConfig instance;

    public volatile boolean enabled = false;
    public transient volatile String apiKey = "";
    private transient String legacyApiKeyFallback = "";
    private transient ApiKeySecretStore secretStore;
    public Map<String, String> rules = java.util.Collections.synchronizedMap(new LinkedHashMap<>());
    public volatile boolean translateTooltips = true;
    public volatile boolean translateSystemMessages = true;
    public volatile boolean showWaitMessage = true; // ★追加: 翻訳中メッセージを表示するか
    public List<String> ignoreWords = java.util.Collections.synchronizedList(new ArrayList<>());
    public volatile String trTargetLang = "EN-US";

    // Hypixel フィルタ設定
    // チャンネルモード: "OFF" / "AUTO" / "CLICK"
    public volatile boolean hypixelMode = false;
    public String hypixelParty    = "AUTO";
    public String hypixelGuild    = "AUTO";
    public String hypixelDM       = "CLICK";
    public String hypixelAllChat  = "CLICK";
    public String receiveTargetLang = "JA";
    public String uiLanguage = "AUTO";

    private static final Set<String> VALID_LANGS = Set.of(
            "AR", "BG", "CS", "DA", "DE", "EL", "EN", "EN-GB", "EN-US",
            "ES", "ET", "FI", "FR", "HU", "ID", "IT", "JA", "KO",
            "LT", "LV", "NB", "NL", "PL", "PT", "PT-BR", "PT-PT",
            "RO", "RU", "SK", "SL", "SV", "TR", "UK", "ZH-HANS", "ZH-HANT",
            "TH", "VI",
            "AUTO",
            "JA_JP", "EN_US", "ZH_HANS", "FR_FR", "ES_ES", "PT_BR", "KO_KR", "TH_TH", "DE_DE", "VI_VN"
    );

    public static boolean isValidLang(String lang) {
        if (lang == null || lang.isBlank()) return false;
        return VALID_LANGS.contains(lang.trim().toUpperCase());
    }

    private TranslatorConfig() {}

    public static synchronized TranslatorConfig getInstance() {
        if (instance == null) instance = load();
        return instance;
    }

    private static TranslatorConfig load() {
        String legacyApiKey = "";
        TranslatorConfig loaded = null;

        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) throw new IllegalStateException("Config root is not an object");

                JsonObject object = element.getAsJsonObject();
                if (object.has("apiKey") && !object.get("apiKey").isJsonNull()) {
                    legacyApiKey = object.get("apiKey").getAsString().trim();
                }
                loaded = GSON.fromJson(object, TranslatorConfig.class);
            } catch (Exception e) { // IOExceptionだけでなくSyntaxErrorなどもキャッチ
                System.err.println("[TranslatorMod] Failed to load config: " + e.getMessage());
                backupCorruptedConfig();
            }
        }

        if (loaded == null) loaded = new TranslatorConfig();
        loaded.initializeSecretStore();
        loaded.validate();
        loaded.loadApiKey(legacyApiKey);
        if (!Files.exists(CONFIG_PATH)) loaded.save();
        return loaded;
    }

    private void initializeSecretStore() {
        if (secretStore == null) secretStore = new ApiKeySecretStore(SECRET_PATH);
    }

    private void loadApiKey(String legacyApiKey) {
        SecretStoreResult result = secretStore.load();
        if (result.status() == SecretStoreResult.Status.LOADED) {
            apiKey = result.secret().trim();
            if (!legacyApiKey.isBlank()) save();
            return;
        }

        if (!legacyApiKey.isBlank()) {
            apiKey = legacyApiKey;
            legacyApiKeyFallback = legacyApiKey;

            if (result.status() == SecretStoreResult.Status.MISSING
                    || result.status() == SecretStoreResult.Status.INVALID) {
                SecretStoreResult repaired = secretStore.save(legacyApiKey);
                if (repaired.status() == SecretStoreResult.Status.SAVED) {
                    legacyApiKeyFallback = "";
                    save();
                    return;
                }
            }

            EncryptionNotice.enqueue(result.status() == SecretStoreResult.Status.DEVICE_ID_UNAVAILABLE
                    ? "translatormod.security.device_id_unavailable"
                    : "translatormod.security.legacy_fallback");
            return;
        }

        if (result.status() == SecretStoreResult.Status.DEVICE_ID_UNAVAILABLE) {
            EncryptionNotice.enqueue("translatormod.security.device_id_unavailable");
        } else if (result.status() == SecretStoreResult.Status.INVALID) {
            EncryptionNotice.enqueue("translatormod.security.decrypt_failed");
        }
    }

    private static void backupCorruptedConfig() {
        try {
            Path backupPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".bak");
            String configContents = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            String sanitized = LEGACY_API_KEY_FIELD.matcher(configContents).replaceAll("");
            sanitized = MALFORMED_LEGACY_API_KEY_FIELD.matcher(sanitized).replaceAll("");
            Files.writeString(CONFIG_PATH, sanitized, StandardCharsets.UTF_8);
            Files.move(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[TranslatorMod] Corrupted config sanitized and moved to " + backupPath);
        } catch (IOException e) {
            System.err.println("[TranslatorMod] Failed to backup corrupted config: " + e.getMessage());
        }
    }

    private static final Set<String> VALID_CHANNEL_MODES = Set.of("AUTO", "CLICK", "OFF");
    private static final Set<String> VALID_UI_LANGUAGES  = Set.of(
            "AUTO", "ja_jp", "en_us", "zh_hans", "fr_fr", "es_es", "pt_br", "ko_kr", "th_th", "de_de", "vi_vn"
    );

    /** 読み込み後の値チェックと補正 */
    void validate() {
        if (apiKey == null) apiKey = "";
        if (rules == null) {
            rules = java.util.Collections.synchronizedMap(new LinkedHashMap<>());
        } else {
            rules = java.util.Collections.synchronizedMap(new LinkedHashMap<>(rules));
        }
        if (ignoreWords == null) {
            ignoreWords = java.util.Collections.synchronizedList(new ArrayList<>());
        } else {
            ignoreWords = java.util.Collections.synchronizedList(new ArrayList<>(ignoreWords));
        }
        apiKey = apiKey.trim();

        // 言語コードが不正な場合はデフォルト値に戻す
        if (!isValidLang(receiveTargetLang)) receiveTargetLang = "JA";
        if (!isValidLang(trTargetLang))      trTargetLang      = "EN-US";

        // UIランゲージが不正な場合はAUTOに戻す
        if (uiLanguage == null || !VALID_UI_LANGUAGES.contains(uiLanguage.toLowerCase()))
            uiLanguage = "AUTO";

        // Hypixelチャンネルモードが不正な場合はデフォルト値に戻す
        if (!VALID_CHANNEL_MODES.contains(hypixelParty))   hypixelParty   = "AUTO";
        if (!VALID_CHANNEL_MODES.contains(hypixelGuild))   hypixelGuild   = "AUTO";
        if (!VALID_CHANNEL_MODES.contains(hypixelDM))      hypixelDM      = "CLICK";
        if (!VALID_CHANNEL_MODES.contains(hypixelAllChat) || "OFF".equals(hypixelAllChat)) hypixelAllChat = "CLICK";
    }

    public synchronized boolean setApiKey(String key) {
        if (key == null) key = "";
        key = key.trim();

        if (key.isEmpty()) {
            boolean deleted = secretStore.delete();
            if (!deleted) {
                EncryptionNotice.enqueue("translatormod.security.save_failed");
                return false;
            }
            this.apiKey = "";
            this.legacyApiKeyFallback = "";
            save();
            return true;
        }

        SecretStoreResult result = secretStore.save(key);
        this.apiKey = key;
        if (result.status() == SecretStoreResult.Status.SAVED) {
            this.legacyApiKeyFallback = "";
            save();
            return true;
        }

        EncryptionNotice.enqueue(result.status() == SecretStoreResult.Status.DEVICE_ID_UNAVAILABLE
                ? "translatormod.security.device_id_unavailable"
                : "translatormod.security.save_failed");
        return false;
    }

    public synchronized void save() {
        try {
            java.nio.file.Path tmpPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject object = JsonParser.parseString(GSON.toJson(this)).getAsJsonObject();
            if (legacyApiKeyFallback.isBlank()) object.remove("apiKey");
            else object.addProperty("apiKey", legacyApiKeyFallback);
            try (Writer writer = Files.newBufferedWriter(tmpPath)) {
                GSON.toJson(object, writer);
            }
            Files.move(tmpPath, CONFIG_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[TranslatorMod] Failed to save config: " + e.getMessage());
        }
    }

    public void putRule(String sourceLang, String targetLang) {
        rules.put(sourceLang.toUpperCase(), targetLang.toUpperCase());
        save();
    }

    public boolean removeRule(String sourceLang) {
        boolean removed = rules.remove(sourceLang.toUpperCase()) != null;
        if (removed) save();
        return removed;
    }

    public void addIgnoreWord(String word) {
        String w = word.trim();
        if (!w.isEmpty() && ignoreWords.stream().noneMatch(s -> s.equalsIgnoreCase(w))) {
            ignoreWords.add(w);
            save();
        }
    }

    public void removeIgnoreWord(String word) {
        boolean removed = ignoreWords.removeIf(s -> s.equalsIgnoreCase(word.trim()));
        if (removed) save();
    }

    public boolean isIgnored(String text) {
        return ignoreWords.stream().anyMatch(w -> w.equalsIgnoreCase(text.trim()));
    }

    public String resolveTargetLang() {
        if (receiveTargetLang != null && !receiveTargetLang.isBlank()) return receiveTargetLang;
        // 旧設定からの移行フォールバック
        if (!rules.isEmpty()) {
            if (rules.containsKey("AUTO")) return rules.get("AUTO");
            return rules.values().iterator().next();
        }
        return null;
    }


}

