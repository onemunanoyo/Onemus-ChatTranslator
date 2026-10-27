package onemunanoyo.translatormod.config.encryption;

import java.io.IOException;
import java.util.List;

public interface CommandExecutor {
    String execute(List<String> command) throws IOException, InterruptedException;
}

