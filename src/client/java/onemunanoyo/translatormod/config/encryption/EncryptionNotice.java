package onemunanoyo.translatormod.config.encryption;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class EncryptionNotice {
    private static final ConcurrentLinkedQueue<Notice> PENDING = new ConcurrentLinkedQueue<>();

    private EncryptionNotice() {}

    public static void enqueue(String translationKey, Object... args) {
        PENDING.add(new Notice(translationKey, args));
    }

    public static Notice poll() {
        return PENDING.poll();
    }

    public static final class Notice {
        private final String translationKey;
        private final Object[] args;

        private Notice(String translationKey, Object[] args) {
            this.translationKey = translationKey;
            this.args = args;
        }

        public String translationKey() {
            return translationKey;
        }

        public Object[] args() {
            return args;
        }
    }
}

