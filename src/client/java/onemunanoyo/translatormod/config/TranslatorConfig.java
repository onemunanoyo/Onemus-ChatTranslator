package onemunanoyo.translatormod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TranslatorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("translatormod.json");

    private static TranslatorConfig instance;

    public boolean enabled = false;
    public String apiKey = "";
    public Map<String, String> rules = new LinkedHashMap<>();
    public boolean translateTooltips = true;
    public boolean translateSystemMessages = true;
    public boolean showWaitMessage = true; // ★追加: 翻訳中メッセージを表示するか
    public List<String> ignoreWords = new ArrayList<>();
    public String trTargetLang = "EN";

    // Hypixel フィルタ設定
    // チャンネルモード: "OFF" / "AUTO" / "CLICK"
    public boolean hypixelMode = false;
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
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                TranslatorConfig loaded = GSON.fromJson(reader, TranslatorConfig.class);
                if (loaded != null) {
                    // 読み込み成功したらバリデーションして返す
                    loaded.validate();
                    return loaded;
                }
            } catch (Exception e) { // IOExceptionだけでなくSyntaxErrorなどもキャッチ
                System.err.println("[TranslatorMod] Failed to load config: " + e.getMessage());
                backupCorruptedConfig();
            }
        }
        TranslatorConfig defaults = new TranslatorConfig();
        defaults.save();
        return defaults;
    }

    private static void backupCorruptedConfig() {
        try {
            Path backupPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".bak");
            Files.move(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[TranslatorMod] Corrupted config moved to " + backupPath);
        } catch (IOException e) {
            System.err.println("[TranslatorMod] Failed to backup corrupted config: " + e.getMessage());
        }
    }

    private static final Set<String> VALID_CHANNEL_MODES = Set.of("AUTO", "CLICK", "OFF");
    private static final Set<String> VALID_UI_LANGUAGES  = Set.of(
            "AUTO", "ja_jp", "en_us", "zh_hans", "fr_fr", "es_es", "pt_br", "ko_kr", "th_th", "de_de", "vi_vn"
    );

    /** 読み込み後の値チェックと補正 */
    private void validate() {
        if (apiKey == null) apiKey = "";
        if (rules == null) rules = new LinkedHashMap<>();
        if (ignoreWords == null) ignoreWords = new ArrayList<>();
        apiKey = apiKey.trim();

        // 言語コードが不正な場合はデフォルト値に戻す
        if (!isValidLang(receiveTargetLang)) receiveTargetLang = "JA";
        if (!isValidLang(trTargetLang))      trTargetLang      = "EN";

        // UIランゲージが不正な場合はAUTOに戻す
        if (uiLanguage == null || !VALID_UI_LANGUAGES.contains(uiLanguage.toLowerCase()))
            uiLanguage = "AUTO";

        // Hypixelチャンネルモードが不正な場合はデフォルト値に戻す
        if (!VALID_CHANNEL_MODES.contains(hypixelParty))   hypixelParty   = "AUTO";
        if (!VALID_CHANNEL_MODES.contains(hypixelGuild))   hypixelGuild   = "AUTO";
        if (!VALID_CHANNEL_MODES.contains(hypixelDM))      hypixelDM      = "CLICK";
        if (!VALID_CHANNEL_MODES.contains(hypixelAllChat) || "OFF".equals(hypixelAllChat)) hypixelAllChat = "CLICK";
    }

    public void setApiKey(String key) {
        if (key == null) key = "";
        this.apiKey = key.trim();
        save();
    }

    public synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
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

    public String resolveTargetLang(String detectedSourceLang) {
        if (rules.isEmpty()) return null;
        String upper = detectedSourceLang == null ? null : detectedSourceLang.toUpperCase();
        if (upper != null && rules.containsKey(upper)) return rules.get(upper);
        if (rules.containsKey("AUTO")) return rules.get("AUTO");
        return null;
    }
}