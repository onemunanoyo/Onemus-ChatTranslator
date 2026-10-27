package onemunanoyo.translatormod.config.encryption.mac;

import onemunanoyo.translatormod.config.encryption.CommandExecutor;
import onemunanoyo.translatormod.config.encryption.DeviceIdException;
import onemunanoyo.translatormod.config.encryption.DeviceIdProvider;
import onemunanoyo.translatormod.config.encryption.ProcessCommandExecutor;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacDeviceIdProvider implements DeviceIdProvider {
    private static final Pattern PLATFORM_UUID = Pattern.compile(
            "\\\"IOPlatformUUID\\\"\\s*=\\s*\\\"([^\\\"]+)\\\"");
    private final CommandExecutor commandExecutor;

    public MacDeviceIdProvider() {
        this(new ProcessCommandExecutor());
    }

    public MacDeviceIdProvider(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String getDeviceId() throws DeviceIdException {
        try {
            String output = commandExecutor.execute(List.of(
                    "ioreg", "-rd1", "-c", "IOPlatformExpertDevice"));
            Matcher matcher = PLATFORM_UUID.matcher(output);
            if (!matcher.find()) throw new DeviceIdException("IOPlatformUUID was not found");
            String deviceId = matcher.group(1).trim();
            if (deviceId.isEmpty()) throw new DeviceIdException("IOPlatformUUID was empty");
            return deviceId;
        } catch (IOException | InterruptedException e) {
            throw new DeviceIdException("Failed to read macOS IOPlatformUUID", e);
        }
    }
}

