package onemunanoyo.translatormod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import onemunanoyo.translatormod.config.TranslatorConfig;

import onemunanoyo.translatormod.util.I18n;
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

        String targetLang = config.resolveTargetLang();
        if (targetLang == null) return CompletableFuture.completedFuture(null);

        String cacheKey = targetLang + ":" + text;
        String cached = cache.get(cacheKey);
        if (cached != null) {
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
                if (detectedLang.equalsIgnoreCase(targetLang)) {
                    return null;
                }
                if (translated.equalsIgnoreCase(text)) {
                    return null;
                }

                // 成功したのでバックオフをリセット
                rateLimitBackoffSec = 30;
                cache.put(cacheKey, translated);
                return translated;

            } catch (DeepLException e) {
                return handleDeepLError(e);
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
        System.out.println("[DEBUG_LOG] translateAsync(text, lang) called -> " + targetLang);
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

        String cacheKey = targetLang + ":" + text;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        if (!inProgress.add(cacheKey)) {
            return CompletableFuture.completedFuture(I18n.getString("translatormod.command.trt.waiting"));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] result = callDeepLApi(text, targetLang, config.apiKey);
                String translated   = result[0];
                String detectedLang = result[1];

                if (detectedLang.equalsIgnoreCase(targetLang)) {
                    return null;
                }
                if (translated.equalsIgnoreCase(text)) {
                    return null;
                }

                rateLimitBackoffSec = 30;
                cache.put(cacheKey, translated);
                return translated;

            } catch (DeepLException e) {
                return handleDeepLError(e);
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
        String cacheKey = targetLang + ":" + text;
        
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);
        
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

                System.out.println("[DEBUG_LOG] fetchUsageAsync: code=" + response.statusCode());

                if (response.statusCode() == 401) {
                    return I18n.getString("translatormod.usage.error.auth");
                }
                if (response.statusCode() == 403) {
                    return I18n.getString("translatormod.usage.error.forbidden");
                }
                if (response.statusCode() != 200) {
                    return I18n.getString("translatormod.usage.error.http", response.statusCode());
                }

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                long used  = json.get("character_count").getAsLong();
                long limit = json.get("character_limit").getAsLong();
                double pct = limit > 0 ? used * 100.0 / limit : 0.0;

                return I18n.getString("translatormod.usage.info", used, limit, pct);

            } catch (java.net.http.HttpTimeoutException e) {
                // タイムアウト時はクールダウンをリセットして即再試行できるようにする
                usageCooldownUntil = 0L;
                return I18n.getString("translatormod.usage.error.timeout");
            } catch (Exception e) {
                usageCooldownUntil = 0L;
                System.err.println("[TranslatorMod] Usage fetch error: " + e.getMessage());
                return I18n.getString("translatormod.usage.error.failed");
            }
        });
    }

    /** 使用量クールダウンの残り秒数（0 = 押せる状態）*/
    public static int getUsageCooldownRemaining() {
        long remaining = (usageCooldownUntil - System.currentTimeMillis()) / 1000 + 1;
        return (int) Math.max(0, remaining);
    }

    // ── 内部処理 ────────────────────────────────────────────────────────────────

    private static String handleDeepLError(DeepLException e) {
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
        System.out.println("[DEBUG_LOG] callDeepLApi: lang=" + targetLang);
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

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject translation = json.getAsJsonArray("translations")
                .get(0).getAsJsonObject();

        return new String[]{
                translation.get("text").getAsString(),
                translation.get("detected_source_language").getAsString()
        };
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
}