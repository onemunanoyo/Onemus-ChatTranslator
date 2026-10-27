package onemunanoyo.translatormod.config.encryption.win;

import onemunanoyo.translatormod.config.encryption.CommandExecutor;
import onemunanoyo.translatormod.config.encryption.DeviceIdException;
import onemunanoyo.translatormod.config.encryption.DeviceIdProvider;
import onemunanoyo.translatormod.config.encryption.ProcessCommandExecutor;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsDeviceIdProvider implements DeviceIdProvider {
    private static final Pattern MACHINE_GUID = Pattern.compile(
            "(?im)^\\s*MachineGuid\\s+REG_\\S+\\s+(.+?)\\s*$");
    private final CommandExecutor commandExecutor;

    public WindowsDeviceIdProvider() {
        this(new ProcessCommandExecutor());
    }

    public WindowsDeviceIdProvider(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String getDeviceId() throws DeviceIdException {
        try {
            String output = commandExecutor.execute(List.of(
                    "reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"));
            Matcher matcher = MACHINE_GUID.matcher(output);
            if (!matcher.find()) throw new DeviceIdException("MachineGuid was not found");
            String deviceId = matcher.group(1).trim();
            if (deviceId.isEmpty()) throw new DeviceIdException("MachineGuid was empty");
            return deviceId;
        } catch (IOException | InterruptedException e) {
            throw new DeviceIdException("Failed to read Windows MachineGuid", e);
        }
    }
}

