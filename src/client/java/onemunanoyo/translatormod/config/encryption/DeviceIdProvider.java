package onemunanoyo.translatormod.config.encryption;

public interface DeviceIdProvider {
    String getDeviceId() throws DeviceIdException;
}

