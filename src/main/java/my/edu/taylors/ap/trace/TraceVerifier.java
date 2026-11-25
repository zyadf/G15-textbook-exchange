package my.edu.taylors.ap.trace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Verifies the hash chain of trace files. Run before submitting:
 * <pre>mvn -q compile exec:java -Dexec.mainClass=my.edu.taylors.ap.trace.TraceVerifier -Dexec.args="runtime/traces"</pre>
 * Your tutor runs exactly the same check.
 */
public final class TraceVerifier {

    private static final String HASH_MARK = ",\"hash\":\"";
    private static final Pattern SEQ = Pattern.compile("^\\{\"seq\":(\\d+),");
    private static final Pattern PREV = Pattern.compile(",\"prev\":\"([0-9a-f]{64})\"$");
    private static final Pattern WM = Pattern.compile(",\"wm\":\"([^\"]*)\",");

    public record ParsedLine(long seq, String watermark, String prev, String hash, String body) { }

    public record Report(Path file, int lines, List<String> problems, List<String> watermarks) {
        public boolean ok() { return problems.isEmpty(); }
    }

    private TraceVerifier() { }

    public static ParsedLine parse(String line) {
        int idx = line.lastIndexOf(HASH_MARK);
        if (idx < 0 || !line.endsWith("\"}")) {
            throw new IllegalArgumentException("Not a trace line (no hash field)");
        }
        String body = line.substring(0, idx);
        String hash = line.substring(idx + HASH_MARK.length(), line.length() - 2);
        Matcher s = SEQ.matcher(body);
        Matcher p = PREV.matcher(body);
        Matcher w = WM.matcher(body);
        if (!s.find() || !p.find()) {
            throw new IllegalArgumentException("Trace line is missing seq or prev");
        }
        return new ParsedLine(Long.parseLong(s.group(1)), w.find() ? w.group(1) : "", p.group(1), hash, body);
    }

    public static Report verify(Path file) throws IOException {
        List<String> problems = new ArrayList<>();
        List<String> watermarks = new ArrayList<>();
        String expectedPrev = TraceLogger.GENESIS;
        long expectedSeq = 1;
        int n = 0;
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            n++;
            ParsedLine pl;
            try {
                pl = parse(line);
            } catch (IllegalArgumentException e) {
                problems.add("line " + n + ": " + e.getMessage());
                continue;
            }
            if (!TraceLogger.sha256(pl.body()).equals(pl.hash())) {
                problems.add("line " + n + ": content was modified (hash mismatch)");
            }
            if (!pl.prev().equals(expectedPrev)) {
                problems.add("line " + n + ": chain broken (a line before it was removed, reordered or edited)");
            }
            if (pl.seq() != expectedSeq) {
                problems.add("line " + n + ": sequence " + pl.seq() + " but expected " + expectedSeq);
            }
            if (!watermarks.contains(pl.watermark())) {
                watermarks.add(pl.watermark());
            }
            expectedPrev = pl.hash();
            expectedSeq = pl.seq() + 1;
        }
        return new Report(file, n, problems, watermarks);
    }

    public static void main(String[] args) throws IOException {
        Path target = Path.of(args.length > 0 ? args[0] : "runtime/traces");
        List<Path> files;
        if (Files.isDirectory(target)) {
            try (Stream<Path> s = Files.list(target)) {
                files = s.filter(f -> f.toString().endsWith(".jsonl")).sorted().toList();
            }
        } else {
            files = List.of(target);
        }
        boolean allOk = true;
        for (Path f : files) {
            Report r = verify(f);
            allOk &= r.ok();
            System.out.printf("%-40s %5d lines  %s  watermark(s)=%s%n", f.getFileName(), r.lines(),
                    r.ok() ? "OK" : "FAILED", r.watermarks());
            r.problems().forEach(p -> System.out.println("    - " + p));
        }
        if (!allOk) {
            System.exit(1);
        }
    }
}
