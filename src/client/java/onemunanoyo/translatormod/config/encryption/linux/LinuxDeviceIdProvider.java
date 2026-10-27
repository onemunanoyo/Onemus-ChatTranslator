package onemunanoyo.translatormod.config.encryption.linux;

import onemunanoyo.translatormod.config.encryption.DeviceIdException;
import onemunanoyo.translatormod.config.encryption.DeviceIdProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LinuxDeviceIdProvider implements DeviceIdProvider {
    private static final List<Path> DEVICE_ID_PATHS = List.of(
            Path.of("/etc/machine-id"),
            Path.of("/var/lib/dbus/machine-id")
    );

    private final List<Path> deviceIdPaths;

    public LinuxDeviceIdProvider() {
        this(DEVICE_ID_PATHS);
    }

    public LinuxDeviceIdProvider(List<Path> deviceIdPaths) {
        this.deviceIdPaths = List.copyOf(deviceIdPaths);
    }

    @Override
    public String getDeviceId() throws DeviceIdException {
        IOException lastFailure = null;
        for (Path path : deviceIdPaths) {
            try {
                String deviceId = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (!deviceId.isEmpty()) return deviceId;
            } catch (IOException e) {
                lastFailure = e;
            }
        }
        throw new DeviceIdException("Linux machine-id could not be read", lastFailure);
    }
}

