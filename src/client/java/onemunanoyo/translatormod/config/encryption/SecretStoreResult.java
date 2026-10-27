package onemunanoyo.translatormod.config.encryption;

public final class SecretStoreResult {
    public enum Status {
        MISSING,
        LOADED,
        SAVED,
        DEVICE_ID_UNAVAILABLE,
        INVALID,
        FAILED
    }

    private final Status status;
    private final String secret;
    private final String detail;

    private SecretStoreResult(Status status, String secret, String detail) {
        this.status = status;
        this.secret = secret;
        this.detail = detail;
    }

    public static SecretStoreResult missing() {
        return new SecretStoreResult(Status.MISSING, "", "");
    }

    public static SecretStoreResult loaded(String secret) {
        return new SecretStoreResult(Status.LOADED, secret, "");
    }

    public static SecretStoreResult saved() {
        return new SecretStoreResult(Status.SAVED, "", "");
    }

    public static SecretStoreResult error(Status status, String detail) {
        return new SecretStoreResult(status, "", detail == null ? "" : detail);
    }

    public Status status() {
        return status;
    }

    public String secret() {
        return secret;
    }

    public String detail() {
        return detail;
    }

    public boolean succeeded() {
        return status == Status.LOADED || status == Status.SAVED;
    }
}

