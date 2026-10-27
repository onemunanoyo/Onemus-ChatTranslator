package onemunanoyo.translatormod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import onemunanoyo.translatormod.config.TranslatorConfig;

import onemunanoyo.translatormod.util.I18n;
import onemunanoyo.translatormod.util.DebugLog;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TranslationHandler {

    /** エラー文字列の先頭に付与するセンチネル。翻訳結果には絶対に含まれない。 */
    public static final String ERR_SENTINEL = "\u0000";

    private static final int CACHE_SIZE = 200;
    private static final Map<String, String> cache = Collections.synchronizedMap(new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_SIZE;
        }
    });
    private static final Set<String> inProgress = Collections.synchronizedSet(new HashSet<>());

    // ── エラー状態管理 ──────────────────────────────────────────────────────────
    // 401/456 など「続けてもムダ」なエラーが出たら翻訳を一時停止する
    private static volatile boolean suspended = false;
    private static volatile String suspendReason = null;

    // 429 レート制限：次にリクエストできる時刻（epoch ms）
    private static volatile long rateLimitUntil = 0L;
    // 429 が続いたときのバックオフ秒数（初回30秒、最大5分）
    private static volatile int rateLimitBackoffSec = 30;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))   // 接続タイムアウト5秒
            .build();

    // ── 公開メソッド ────────────────────────────────────────────────────────────

    public static CompletableFuture<String> translateAsync(String text) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        if (!config.enabled || config.apiKey.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        // 停止中（APIキー無効・文字数上限など）
        if (suspended) {
            return CompletableFuture.completedFuture(ERR_SENTINEL + I18n.getString("translatormod.chat.error.suspended_with_reason", suspendReason));
        }

        // レート制限中
        if (System.currentTimeMillis() < rateLimitUntil) {
            long waitSec = (rateLimitUntil - System.currentTimeMillis()) / 1000 + 1;
            return CompletableFuture.completedFuture(ERR_SENTINEL + I18n.getString("translatormod.chat.error.rate_limit_with_time", waitSec));
        }

        // 無視ワード・スキップチェック
        String textToCheck = text.replaceAll("^<[^>]+>\\s*", "").trim();
        if (config.isIgnored(textToCheck))              return CompletableFuture.completedFuture(null);
        if (textToCheck.matches("^[0-9\\s]+$"))         return CompletableFuture.completedFuture(null);
        if (textToCheck.length() <= 2)                  return CompletableFuture.completedFuture(null);

        String rawTargetLang = config.resolveTargetLang();
        if (rawTargetLang == null) return CompletableFuture.completedFuture(null);
        String targetLang = normalizeLanguageCode(rawTargetLang);

        String cacheKey = targetLang + ":" + text;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            if (cached.equals(text)) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(cached);
        }
        
        // すでに翻訳中なら重複リクエストを避ける
        if (!inProgress.add(cacheKey)) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] result = callDeepLApi(text, targetLang, config.apiKey);
                String translated  = result[0];
                String detectedLang = result[1];

                // 検知言語 == ターゲット言語なら表示しない
                if (detectedLang.equalsIgnoreCase(targetLang) || translated.equalsIgnoreCase(text)) {
                    cache.put(cacheKey, text); // キャッシュに原文を入れて「翻訳不要」とマーク
                    return null;
                }

                // 成功したのでバックオフをリセット
                rateLimitBackoffSec = 30;
                cache.put(cacheKey, translated);
                return translated;

            } catch (DeepLException e) {
                return handleDeepLError(e);
            } catch (ParseException e) {
                System.err.println("[TranslatorMod] API Parse error: " + e.getMessage());
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.parse");
            } catch (Exception e) {
                // ネットワーク切断・タイムアウトなど
                System.err.println("[TranslatorMod] Network error: " + e.getMessage());
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.network");
            } finally {
                // Error を含むあらゆる例外でも必ず解放する
                inProgress.remove(cacheKey);
            }
        });
    }

    /**
     * ターゲット言語を明示して翻訳する（/tr コマンド用）。
     * enabled チェック・無視ワードチェックはスキップし、APIキーと停止状態のみ確認する。
     */
    public static CompletableFuture<String> translateAsync(String text, String targetLang) {
        DebugLog.log("translateAsync(text, lang) called -> " + targetLang);
        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        if (suspended) {
            return CompletableFuture.completedFuture(ERR_SENTINEL + I18n.getString("translatormod.chat.error.suspended_with_reason", suspendReason));
        }
        if (System.currentTimeMillis() < rateLimitUntil) {
            long waitSec = (rateLimitUntil - System.currentTimeMillis()) / 1000 + 1;
            return CompletableFuture.completedFuture(ERR_SENTINEL + I18n.getString("translatormod.chat.error.rate_limit_with_time", waitSec));
        }

        String normalizedTargetLang = normalizeLanguageCode(targetLang);
        String cacheKey = normalizedTargetLang + ":" + text;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            if (cached.equals(text)) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(cached);
        }
        if (!inProgress.add(cacheKey)) {
            return CompletableFuture.completedFuture(I18n.getString("translatormod.command.trt.waiting"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] result = callDeepLApi(text, normalizedTargetLang, config.apiKey);
                String translated   = result[0];
                String detectedLang = result[1];

                if (detectedLang.equalsIgnoreCase(normalizedTargetLang) || translated.equalsIgnoreCase(text)) {
                    cache.put(cacheKey, text); // キャッシュに原文を入れて「翻訳不要」とマーク
                    return null;
                }

                rateLimitBackoffSec = 30;
                cache.put(cacheKey, translated);
                return translated;

            } catch (DeepLException e) {
                return handleDeepLError(e);
            } catch (ParseException e) {
                System.err.println("[TranslatorMod] API Parse error: " + e.getMessage());
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.parse");
            } catch (Exception e) {
                System.err.println("[TranslatorMod] Network error: " + e.getMessage());
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.network");
            } finally {
                inProgress.remove(cacheKey);
            }
        });
    }

    public static String getCachedTranslation(String text) {
        TranslatorConfig config = TranslatorConfig.getInstance();
        if (!config.enabled || config.apiKey.isBlank()) return null;
        String textToCheck = text.replaceAll("^<[^>]+>\\s*", "").trim();
        if (config.isIgnored(textToCheck))      return null;
        if (textToCheck.matches("^[0-9\\s]+$")) return null;
        if (textToCheck.length() <= 2)          return null;
        String targetLang = config.resolveTargetLang();
        if (targetLang == null) return null;
        targetLang = normalizeLanguageCode(targetLang);
        String cacheKey = targetLang + ":" + text;
        
        String cached = cache.get(cacheKey);
        if (cached != null) {
            if (cached.equals(text)) {
                return null;
            }
            return cached;
        }
        
        // 翻訳中なら新たにリクエストしない（フリーズ防止）
        if (inProgress.contains(cacheKey)) return null;

        translateAsync(text);
        return null;
    }

    /** 停止状態・レート制限・バックオフをすべてリセットする */
    public static void clearCache() {
        cache.clear();
        suspended = false;
        suspendReason = null;
        rateLimitUntil = 0L;
        rateLimitBackoffSec = 30;
    }

    /** 現在の停止理由を返す（null = 正常）*/
    public static String getSuspendReason() {
        return suspended ? suspendReason : null;
    }

    // ── 使用量取得 ────────────────────────────────────────────────────────────
    // クールダウン管理（画面側ではなくここで持つ）
    private static volatile long usageCooldownUntil = 0L;
    private static final int USAGE_COOLDOWN_SEC = 30;

    private static volatile long validationCooldownUntil = 0L;
    private static final int VALIDATION_COOLDOWN_SEC = 30;

    /**
     * 使用量を非同期取得する。
     * @return "使用済: 12,345 / 500,000 文字 (2.5%)" のような文字列、
     *         クールダウン中・エラー時はそれを示す文字列
     */
    public static CompletableFuture<String> fetchUsageAsync() {
        TranslatorConfig config = TranslatorConfig.getInstance();
        if (config.apiKey.isBlank()) {
            return CompletableFuture.completedFuture(I18n.getString("translatormod.usage.error.no_api"));
        }

        // チェックと更新をアトミックに行い、TOCTOU競合を防ぐ
        synchronized (TranslationHandler.class) {
            long now = System.currentTimeMillis();
            if (now < usageCooldownUntil) {
                long remaining = (usageCooldownUntil - now) / 1000 + 1;
                return CompletableFuture.completedFuture(I18n.getString("translatormod.usage.error.cooldown", remaining));
            }
            // クールダウン開始
            usageCooldownUntil = now + USAGE_COOLDOWN_SEC * 1000L;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = config.apiKey.endsWith(":fx")
                        ? "https://api-free.deepl.com/v2/usage"
                        : "https://api.deepl.com/v2/usage";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Authorization", "DeepL-Auth-Key " + config.apiKey)
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT
                        .send(request, HttpResponse.BodyHandlers.ofString());

                DebugLog.log("fetchUsageAsync: code=" + response.statusCode());

                if (response.statusCode() == 401) {
                    return I18n.getString("translatormod.usage.error.auth");
                }
                if (response.statusCode() == 403) {
                    return I18n.getString("translatormod.usage.error.forbidden");
                }
                if (response.statusCode() != 200) {
                    return I18n.getString("translatormod.usage.error.http", response.statusCode());
                }

                JsonObject json;
                try {
                    json = JsonParser.parseString(response.body()).getAsJsonObject();
                } catch (Exception e) {
                    throw new ParseException("Malformed JSON: " + e.getMessage());
                }

                if (!json.has("character_count") || !json.has("character_limit")) {
                    throw new ParseException("Missing character_count or character_limit.");
                }

                long used  = json.get("character_count").getAsLong();
                long limit = json.get("character_limit").getAsLong();
                double pct = limit > 0 ? used * 100.0 / limit : 0.0;

                return I18n.getString("translatormod.usage.info", used, limit, pct);

            } catch (java.net.http.HttpTimeoutException e) {
                // タイムアウト時はクールダウンをリセットして即再試行できるようにする
                usageCooldownUntil = 0L;
                return I18n.getString("translatormod.usage.error.timeout");
            } catch (ParseException e) {
                usageCooldownUntil = 0L;
                System.err.println("[TranslatorMod] Usage API Parse error: " + e.getMessage());
                return I18n.getString("translatormod.usage.error.parse");
            } catch (Exception e) {
                usageCooldownUntil = 0L;
                System.err.println("[TranslatorMod] Usage fetch error: " + e.getMessage());
                return I18n.getString("translatormod.usage.error.failed");
            }
        });
    }

    public static CompletableFuture<Boolean> validateApiKeyAsync(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }

        synchronized (TranslationHandler.class) {
            long now = System.currentTimeMillis();
            if (now < validationCooldownUntil) {
                return CompletableFuture.completedFuture(null);
            }
            validationCooldownUntil = now + VALIDATION_COOLDOWN_SEC * 1000L;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = apiKey.endsWith(":fx")
                        ? "https://api-free.deepl.com/v2/usage"
                        : "https://api.deepl.com/v2/usage";

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(endpoint))
                        .header("Authorization", "DeepL-Auth-Key " + apiKey)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                java.net.http.HttpResponse<String> response = HTTP_CLIENT
                        .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                return response.statusCode() == 200;
            } catch (Exception e) {
                synchronized (TranslationHandler.class) {
                    validationCooldownUntil = 0L;
                }
                return false;
            }
        });
    }

    public static int getValidationCooldownRemaining() {
        long remaining = (validationCooldownUntil - System.currentTimeMillis()) / 1000 + 1;
        return (int) Math.max(0, remaining);
    }

    /** 使用量クールダウンの残り秒数（0 = 押せる状態）*/
    public static int getUsageCooldownRemaining() {
        long remaining = (usageCooldownUntil - System.currentTimeMillis()) / 1000 + 1;
        return (int) Math.max(0, remaining);
    }

    // ── 内部処理 ────────────────────────────────────────────────────────────────

    private static synchronized String handleDeepLError(DeepLException e) {
        switch (e.statusCode) {
            case 401:
                // APIキーが無効 → 手動で直してもらうまで停止
                suspended = true;
                suspendReason = I18n.getString("translatormod.chat.error.reason.auth");
                System.err.println("[TranslatorMod] Invalid API key. Translation suspended.");
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.suspended");

            case 429:
                // レート制限 → バックオフして自動復帰
                rateLimitUntil = System.currentTimeMillis() + rateLimitBackoffSec * 1000L;
                System.err.println("[TranslatorMod] Rate limited. Waiting " + rateLimitBackoffSec + "s.");
                rateLimitBackoffSec = Math.min(rateLimitBackoffSec * 2, 300); // 最大5分
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.rate_limit");

            case 456:
                // 月間文字数上限 → 月が変わるまで停止
                suspended = true;
                suspendReason = I18n.getString("translatormod.chat.error.reason.quota");
                System.err.println("[TranslatorMod] Character quota exceeded. Translation suspended.");
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.quota");

            case 503:
                // DeepLサービス一時停止 → 短時間待機
                rateLimitUntil = System.currentTimeMillis() + 60_000L;
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.service_down");

            default:
                System.err.println("[TranslatorMod] DeepL HTTP " + e.statusCode + ": " + e.body);
                return ERR_SENTINEL + I18n.getString("translatormod.chat.error.http", e.statusCode);
        }
    }

    private static String[] callDeepLApi(String text, String targetLang, String apiKey) throws Exception {
        DebugLog.log("callDeepLApi: lang=" + targetLang);
        String endpoint = apiKey.endsWith(":fx")
                ? "https://api-free.deepl.com/v2/translate"
                : "https://api.deepl.com/v2/translate";

        String body = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&target_lang=" + URLEncoder.encode(targetLang, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "DeepL-Auth-Key " + apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(8))   // リクエストタイムアウト8秒
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP_CLIENT
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new DeepLException(response.statusCode(), response.body());
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new ParseException("Malformed JSON: " + e.getMessage());
        }

        if (!json.has("translations")) {
            throw new ParseException("Missing 'translations' key.");
        }

        com.google.gson.JsonArray translations = json.getAsJsonArray("translations");
        if (translations == null || translations.size() == 0) {
            throw new ParseException("Empty 'translations' array.");
        }

        JsonObject translation = translations.get(0).getAsJsonObject();
        if (!translation.has("text") || !translation.has("detected_source_language")) {
            throw new ParseException("Missing 'text' or 'detected_source_language' keys in translation.");
        }

        return new String[]{
                translation.get("text").getAsString(),
                translation.get("detected_source_language").getAsString()
        };
    }

    private static String normalizeLanguageCode(String lang) {
        if (lang == null) return null;
        String upper = lang.toUpperCase().trim().replace('_', '-');
        
        if (upper.equals("EN")) return "EN-US";
        if (upper.equals("PT")) return "PT-BR";
        if (upper.equals("ZH")) return "ZH-HANS";
        
        if (upper.startsWith("JA-") || upper.equals("JA")) return "JA";
        if (upper.startsWith("KO-") || upper.equals("KO")) return "KO";
        if (upper.startsWith("FR-") || upper.equals("FR")) return "FR";
        if (upper.startsWith("DE-") || upper.equals("DE")) return "DE";
        if (upper.startsWith("ES-") || upper.equals("ES")) return "ES";
        if (upper.startsWith("TH-") || upper.equals("TH")) return "TH";
        if (upper.startsWith("VI-") || upper.equals("VI")) return "VI";
        if (upper.startsWith("IT-") || upper.equals("IT")) return "IT";
        if (upper.startsWith("NL-") || upper.equals("NL")) return "NL";
        if (upper.startsWith("PL-") || upper.equals("PL")) return "PL";
        if (upper.startsWith("RU-") || upper.equals("RU")) return "RU";
        if (upper.startsWith("TR-") || upper.equals("TR")) return "TR";
        if (upper.startsWith("UK-") || upper.equals("UK")) return "UK";
        if (upper.startsWith("ID-") || upper.equals("ID")) return "ID";
        if (upper.startsWith("BG-") || upper.equals("BG")) return "BG";
        if (upper.startsWith("CS-") || upper.equals("CS")) return "CS";
        if (upper.startsWith("DA-") || upper.equals("DA")) return "DA";
        if (upper.startsWith("EL-") || upper.equals("EL")) return "EL";
        if (upper.startsWith("ET-") || upper.equals("ET")) return "ET";
        if (upper.startsWith("FI-") || upper.equals("FI")) return "FI";
        if (upper.startsWith("HU-") || upper.equals("HU")) return "HU";
        if (upper.startsWith("LT-") || upper.equals("LT")) return "LT";
        if (upper.startsWith("LV-") || upper.equals("LV")) return "LV";
        if (upper.startsWith("NB-") || upper.equals("NB")) return "NB";
        if (upper.startsWith("RO-") || upper.equals("RO")) return "RO";
        if (upper.startsWith("SK-") || upper.equals("SK")) return "SK";
        if (upper.startsWith("SL-") || upper.equals("SL")) return "SL";
        if (upper.startsWith("SV-") || upper.equals("SV")) return "SV";
        
        return upper;
    }

    // ── 内部例外クラス ───────────────────────────────────────────────────────────
    private static class DeepLException extends Exception {
        final int statusCode;
        final String body;
        DeepLException(int statusCode, String body) {
            super("HTTP " + statusCode);
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static class ParseException extends Exception {
        ParseException(String message) {
            super(message);
        }
    }
}