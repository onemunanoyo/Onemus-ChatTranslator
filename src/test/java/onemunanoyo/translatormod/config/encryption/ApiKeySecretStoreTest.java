package onemunanoyo.translatormod.config.encryption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeySecretStoreTest {
    @TempDir Path directory;

    @Test
    void savesEncryptedKeyAndRejectsDifferentDeviceAndCorruption() throws Exception {
        Path path = directory.resolve("secret.env");
        ApiKeySecretStore store = new ApiKeySecretStore(path, () -> "device-one");
        assertEquals(SecretStoreResult.Status.MISSING, store.load().status());
        assertEquals(SecretStoreResult.Status.SAVED, store.save("test-api-key").status());
        assertFalse(Files.readString(path).contains("test-api-key"));
        assertEquals("test-api-key", store.load().secret());
        assertEquals(SecretStoreResult.Status.INVALID,
                new ApiKeySecretStore(path, () -> "device-two").load().status());
        Files.writeString(path, "TRANSLATORMOD_SECRET_FORMAT=invalid\n");
        assertEquals(SecretStoreResult.Status.INVALID, store.load().status());
        assertTrue(store.delete());
        assertEquals(SecretStoreResult.Status.MISSING, store.load().status());
    }

    @Test
    void reportsUnavailableDeviceId() {
        ApiKeySecretStore store = new ApiKeySecretStore(directory.resolve("secret.env"), () -> {
            throw new DeviceIdException("unavailable");
        });
        assertEquals(SecretStoreResult.Status.DEVICE_ID_UNAVAILABLE, store.save("test-api-key").status());
    }
}
