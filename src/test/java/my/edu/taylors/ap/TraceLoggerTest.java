package my.edu.taylors.ap;

import my.edu.taylors.ap.trace.TraceLogger;
import my.edu.taylors.ap.trace.TraceVerifier;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceLoggerTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T03:00:00Z"), ZoneId.of("Asia/Kuala_Lumpur"));

    @Test
    void untouchedTraceVerifies() throws Exception {
        Path dir = Files.createTempDirectory("trace");
        TraceLogger t = new TraceLogger(dir, "ITS66704-G01-ABCD", clock);
        t.log("tool.call", Map.of("tool", "quoteOrder", "args", "{\"items\":\"D01x2\"}"));
        t.note("line with \"quotes\" and a\nnewline");
        TraceVerifier.Report r = TraceVerifier.verify(t.currentFile());
        assertTrue(r.ok(), String.valueOf(r.problems()));
        assertEquals(2, r.lines());
        assertEquals(List.of("ITS66704-G01-ABCD"), r.watermarks());
    }

    @Test
    void editedLineIsDetected() throws Exception {
        Path dir = Files.createTempDirectory("trace");
        TraceLogger t = new TraceLogger(dir, "ITS66704-G01-ABCD", clock);
        t.log("tool.result", Map.of("result", "TOTAL RM9.40"));
        t.log("note", Map.of("text", "second"));
        List<String> lines = new ArrayList<>(Files.readAllLines(t.currentFile()));
        lines.set(0, lines.get(0).replace("RM9.40", "RM9.45"));
        Files.write(t.currentFile(), lines);
        assertFalse(TraceVerifier.verify(t.currentFile()).ok());
    }

    @Test
    void deletedLineBreaksTheChain() throws Exception {
        Path dir = Files.createTempDirectory("trace");
        TraceLogger t = new TraceLogger(dir, "ITS66704-G01-ABCD", clock);
        t.note("one");
        t.note("two");
        t.note("three");
        List<String> lines = new ArrayList<>(Files.readAllLines(t.currentFile()));
        lines.remove(1);
        Files.write(t.currentFile(), lines);
        assertFalse(TraceVerifier.verify(t.currentFile()).ok());
    }

    @Test
    void restartContinuesTheChain() throws Exception {
        Path dir = Files.createTempDirectory("trace");
        new TraceLogger(dir, "ITS66704-G01-ABCD", clock).note("before restart");
        TraceLogger again = new TraceLogger(dir, "ITS66704-G01-ABCD", clock);
        again.note("after restart");
        TraceVerifier.Report r = TraceVerifier.verify(again.currentFile());
        assertTrue(r.ok(), String.valueOf(r.problems()));
        assertEquals(2, r.lines());
    }
}
