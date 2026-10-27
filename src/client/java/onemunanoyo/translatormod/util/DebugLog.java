package onemunanoyo.translatormod.util;

/**
 * デバッグログユーティリティ。
 * <p>
 * JVM引数 {@code -Dtranslatormod.debug=true} を設定した場合のみ出力される。
 * Minecraftランチャーの「JVM引数」欄に追加するか、
 * 開発時は {@code gradlew runClient -Dtranslatormod.debug=true} で有効化できる。
 * </p>
 *
 * <pre>
 * 有効化例 (ランチャー JVM引数):
 *   -Dtranslatormod.debug=true
 *
 * 有効化例 (Gradle):
 *   .\gradlew.bat runClient -Ptranslatormod.debug=true
 * </pre>
 */
public final class DebugLog {

    private static final boolean DEBUG_ENABLED =
            Boolean.parseBoolean(System.getProperty("translatormod.debug", "false"));

    private DebugLog() {}

    /**
     * デバッグが有効な場合のみ {@code [TranslatorMod DEBUG]} プレフィックス付きで標準出力へ表示する。
     *
     * @param message 出力するメッセージ
     */
    public static void log(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[TranslatorMod DEBUG] " + message);
        }
    }

    /**
     * {@link #log(String)} の書式付きバージョン。{@link String#format} と同じ書式を使う。
     *
     * @param format  書式文字列
     * @param args    書式引数
     */
    public static void log(String format, Object... args) {
        if (DEBUG_ENABLED) {
            System.out.printf("[TranslatorMod DEBUG] " + format + "%n", args);
        }
    }

    /** デバッグが有効かどうかを返す。 */
    public static boolean isEnabled() {
        return DEBUG_ENABLED;
    }
}

