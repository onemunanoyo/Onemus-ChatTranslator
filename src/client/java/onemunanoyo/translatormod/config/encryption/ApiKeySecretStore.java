package onemunanoyo.translatormod.config.encryption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiKeySecretStore {
    private static final String FORMAT_VERSION = "1";
    private static final String FORMAT_KEY = "TRANSLATORMOD_SECRET_FORMAT";
    private static final String SALT_KEY = "TRANSLATORMOD_SECRET_SALT";
    private static final String IV_KEY = "TRANSLATORMOD_SECRET_IV";
    private static final String DATA_KEY = "TRANSLATORMOD_SECRET_DATA";
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;

    private final Path secretPath;
    private final DeviceIdProvider deviceIdProvider;
    private final SecureRandom secureRandom;

    public ApiKeySecretStore(Path secretPath) {
        this(secretPath, createDeviceIdProvider(), new SecureRandom());
    }

    public ApiKeySecretStore(Path secretPath, DeviceIdProvider deviceIdProvider) {
        this(secretPath, deviceIdProvider, new SecureRandom());
    }

    ApiKeySecretStore(Path secretPath, DeviceIdProvider deviceIdProvider, SecureRandom secureRandom) {
        this.secretPath = secretPath;
        this.deviceIdProvider = deviceIdProvider;
        this.secureRandom = secureRandom;
    }

    public SecretStoreResult load() {
        if (!Files.exists(secretPath)) return SecretStoreResult.missing();

        final String deviceId;
        try {
            deviceId = deviceIdProvider.getDeviceId();
        } catch (DeviceIdException e) {
            return SecretStoreResult.error(SecretStoreResult.Status.DEVICE_ID_UNAVAILABLE, e.getMessage());
        }

        try {
            Map<String, String> values = readValues();
            if (!FORMAT_VERSION.equals(values.get(FORMAT_KEY))) {
                return SecretStoreResult.error(SecretStoreResult.Status.INVALID, "Unsupported secret format");
            }

            byte[] salt = decode(values, SALT_KEY, SALT_LENGTH);
            byte[] iv = decode(values, IV_KEY, IV_LENGTH);
            byte[] encrypted = decode(values, DATA_KEY, -1);
            String secret = AesGcmSecretCipher.decrypt(encrypted, deviceId, salt, iv);
            return SecretStoreResult.loaded(secret);
        } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
            return SecretStoreResult.error(SecretStoreResult.Status.INVALID, e.getMessage());
        }
    }

    public SecretStoreResult save(String secret) {
        final String deviceId;
        try {
            deviceId = deviceIdProvider.getDeviceId();
        } catch (DeviceIdException e) {
            return SecretStoreResult.error(SecretStoreResult.Status.DEVICE_ID_UNAVAILABLE, e.getMessage());
        }

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(salt);
        secureRandom.nextBytes(iv);

        try {
            byte[] encrypted = AesGcmSecretCipher.encrypt(secret, deviceId, salt, iv);
            String content = FORMAT_KEY + "=" + FORMAT_VERSION + "\n"
                    + SALT_KEY + "=" + encode(salt) + "\n"
                    + IV_KEY + "=" + encode(iv) + "\n"
                    + DATA_KEY + "=" + encode(encrypted) + "\n";
            writeAtomically(content);
            return SecretStoreResult.saved();
        } catch (IOException | GeneralSecurityException e) {
            return SecretStoreResult.error(SecretStoreResult.Status.FAILED, e.getMessage());
        }
    }

    public boolean delete() {
        try {
            Files.deleteIfExists(secretPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Map<String, String> readValues() throws IOException {
        Map<String, String> values = new HashMap<>();
        List<String> lines = Files.readAllLines(secretPath, StandardCharsets.UTF_8);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int separator = trimmed.indexOf('=');
            if (separator <= 0) throw new IOException("Invalid secret file entry");
            values.put(trimmed.substring(0, separator), trimmed.substring(separator + 1));
        }
        return values;
    }

    private static byte[] decode(Map<String, String> values, String key, int expectedLength) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        byte[] decoded = Base64.getDecoder().decode(value);
        if (expectedLength >= 0 && decoded.length != expectedLength) {
            throw new IllegalArgumentException("Invalid " + key + " length");
        }
        return decoded;
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().withoutPadding().encodeToString(value);
    }

    private void writeAtomically(String content) throws IOException {
        Files.createDirectories(secretPath.getParent());
        Path tmpPath = secretPath.resolveSibling(secretPath.getFileName() + ".tmp");
        Files.writeString(tmpPath, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmpPath, secretPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmpPath, secretPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static DeviceIdProvider createDeviceIdProvider() {
        try {
            return PlatformDeviceIdProvider.current();
        } catch (DeviceIdException e) {
            return () -> { throw e; };
        }
    }
}

