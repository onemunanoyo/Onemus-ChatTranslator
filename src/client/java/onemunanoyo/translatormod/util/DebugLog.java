package onemunanoyo.translatormod.util;

/** デバッグ出力を通常のゲームログから分離するためのユーティリティ。 */
public final class DebugLog {

    private static final boolean DEBUG_ENABLED =
            Boolean.parseBoolean(System.getProperty("translatormod.debug", "false"));

    private DebugLog() {}

    public static void log(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[TranslatorMod DEBUG] " + message);
        }
    }

    public static void log(String format, Object... args) {
        if (DEBUG_ENABLED) {
            System.out.printf("[TranslatorMod DEBUG] " + format + "%n", args);
        }
    }

    public static boolean isEnabled() {
        return DEBUG_ENABLED;
    }
}
