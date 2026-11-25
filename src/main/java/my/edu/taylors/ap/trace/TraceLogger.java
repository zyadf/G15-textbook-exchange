package my.edu.taylors.ap.trace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Append-only, hash-chained JSON Lines (JSONL) trace of everything the agent does:
 * model requests/responses, tool calls, guardrail decisions and errors.
 * <p>
 * DO NOT REMOVE OR BYPASS. The trace files in {@code runtime/traces/} are part of your
 * submission. They are your evidence of the development process and are discussed in the viva.
 * Each line stores the SHA-256 hash of the previous line, so editing or deleting a line
 * breaks the chain and is detected by {@link TraceVerifier}.
 */
public final class TraceLogger {

    public static final String GENESIS = "0".repeat(64);

    private final Path dir;
    private final String watermarkId;
    private final Clock clock;
    private final List<Consumer<String>> observers = new CopyOnWriteArrayList<>();
    private String previousHash = GENESIS;
    private long sequence;
    private Path currentFile;

    public TraceLogger(Path dir, String watermarkId) {
        this(dir, watermarkId, Clock.systemDefaultZone());
    }

    public TraceLogger(Path dir, String watermarkId, Clock clock) {
        this.dir = dir;
        this.watermarkId = watermarkId;
        this.clock = clock;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create trace directory " + dir, e);
        }
    }

    /** Lets the UI show a live "agent trace" panel. Receives a short human-readable summary. */
    public void addObserver(Consumer<String> observer) {
        observers.add(observer);
    }

    /**
     * Appends one event. {@code type} examples: "session", "model.request", "model.response",
     * "tool.call", "tool.result", "guardrail.input", "guardrail.output", "error", "note".
     */
    public synchronized void log(String type, Map<String, ?> data) {
        Path file = fileForToday();
        if (!file.equals(currentFile)) {
            currentFile = file;
            resumeChain(file);
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("seq", ++sequence);
        entry.put("ts", Instant.now(clock).toString());
        entry.put("wm", watermarkId);
        entry.put("type", type);
        entry.put("data", data == null ? Map.of() : data);
        entry.put("prev", previousHash);
        String body = Json.write(entry);                 // ends with '}'
        String bodyOpen = body.substring(0, body.length() - 1);
        String hash = sha256(bodyOpen);
        String line = bodyOpen + ",\"hash\":\"" + hash + "\"}";
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write trace file " + file, e);
        }
        previousHash = hash;
        String summary = type + " " + summarise(data);
        observers.forEach(o -> o.accept(summary));
    }

    public synchronized void note(String message) {
        log("note", Map.of("text", message));
    }

    public Path currentFile() {
        return fileForToday();
    }

    private Path fileForToday() {
        return dir.resolve("trace-" + LocalDate.now(clock) + ".jsonl");
    }

    /** Continues the chain from the last line already in today's file (e.g. after a restart). */
    private void resumeChain(Path file) {
        previousHash = GENESIS;
        sequence = 0;
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String l = lines.get(i).trim();
                if (!l.isEmpty()) {
                    TraceVerifier.ParsedLine parsed = TraceVerifier.parse(l);
                    previousHash = parsed.hash();
                    sequence = parsed.seq();
                    return;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read existing trace " + file, e);
        }
    }

    private static String summarise(Map<String, ?> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        String s = data.toString();
        return s.length() > 160 ? s.substring(0, 157) + "..." : s;
    }

    static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
