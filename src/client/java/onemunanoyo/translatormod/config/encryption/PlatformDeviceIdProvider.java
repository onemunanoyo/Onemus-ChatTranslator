package onemunanoyo.translatormod.config.encryption;

import onemunanoyo.translatormod.config.encryption.linux.LinuxDeviceIdProvider;
import onemunanoyo.translatormod.config.encryption.mac.MacDeviceIdProvider;
import onemunanoyo.translatormod.config.encryption.win.WindowsDeviceIdProvider;

import java.util.Locale;

public final class PlatformDeviceIdProvider {
    private PlatformDeviceIdProvider() {}

    public static DeviceIdProvider current() throws DeviceIdException {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) return new WindowsDeviceIdProvider();
        if (osName.contains("mac")) return new MacDeviceIdProvider();
        if (osName.contains("linux")) return new LinuxDeviceIdProvider();
        throw new DeviceIdException("Unsupported operating system: " + osName);
    }
}

