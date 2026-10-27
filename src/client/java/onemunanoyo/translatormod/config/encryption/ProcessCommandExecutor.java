package onemunanoyo.translatormod.config.encryption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessCommandExecutor implements CommandExecutor {
    private static final long TIMEOUT_SECONDS = 5;

    @Override
    public String execute(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Command timed out");
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new IOException("Command failed with exit code " + process.exitValue());
        }
        return output;
    }
}

