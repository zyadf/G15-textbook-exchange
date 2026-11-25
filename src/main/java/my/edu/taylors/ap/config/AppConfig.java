package my.edu.taylors.ap.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;

/**
 * Reads {@code config.properties} from the project root (the working directory when you run
 * {@code mvn javafx:run}). API keys are NEVER stored in this file: the file names an
 * environment variable and the key is read from the environment.
 */
public record AppConfig(
        int groupNumber,
        Provider provider,
        String modelName,
        String baseUrl,
        String apiKeyEnv,
        double temperature,
        Duration timeout,
        boolean mcpEnabled,
        Path scenariosDir,
        Path runtimeDir,
        String fixedClock) {

    public enum Provider { OLLAMA, LMSTUDIO, OPENAI, ANTHROPIC }

    public static final Path DEFAULT_FILE = Path.of("config.properties");

    public static AppConfig load() {
        return load(DEFAULT_FILE);
    }

    public static AppConfig load(Path file) {
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader in = Files.newBufferedReader(file)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + file.toAbsolutePath(), e);
            }
        }
        return fromProperties(p);
    }

    static AppConfig fromProperties(Properties p) {
        int group;
        try {
            group = Integer.parseInt(p.getProperty("group.number", "0").trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("group.number in config.properties must be a whole number", e);
        }
        if (group < 1) {
            throw new IllegalStateException("Set group.number in config.properties to your assigned group number.");
        }
        Provider provider = Provider.valueOf(p.getProperty("llm.provider", "OLLAMA").trim().toUpperCase());
        return new AppConfig(
                group,
                provider,
                p.getProperty("llm.model", "qwen3:8b").trim(),
                p.getProperty("llm.baseUrl", "").trim(),
                p.getProperty("llm.apiKeyEnv", "").trim(),
                Double.parseDouble(p.getProperty("llm.temperature", "0.2").trim()),
                Duration.ofSeconds(Long.parseLong(p.getProperty("llm.timeoutSeconds", "120").trim())),
                Boolean.parseBoolean(p.getProperty("mcp.enabled", "false").trim()),
                Path.of(p.getProperty("scenarios.dir", "scenarios").trim()),
                Path.of(p.getProperty("runtime.dir", "runtime").trim()),
                p.getProperty("clock.fixed", "").trim());
    }

    /**
     * The clock every rule uses. Normally the system clock; for testing and for the tutor's
     * marking session, {@code clock.fixed=2026-11-02T15:30} freezes "now" at that local time.
     */
    public Clock clock() {
        if (fixedClock == null || fixedClock.isBlank()) {
            return Clock.systemDefaultZone();
        }
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(LocalDateTime.parse(fixedClock).atZone(zone).toInstant(), zone);
    }

    /** Reads the API key from the environment variable named by {@code llm.apiKeyEnv}. */
    public String apiKey() {
        if (apiKeyEnv.isBlank()) {
            return "";
        }
        String key = System.getenv(apiKeyEnv);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Environment variable " + apiKeyEnv + " is not set. "
                    + "Set it in your terminal/IDE run configuration; never commit keys to git.");
        }
        return key;
    }
}
