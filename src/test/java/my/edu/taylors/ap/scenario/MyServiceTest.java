package my.edu.taylors.ap.scenario;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TODO [B1] Unit tests for YOUR service class. The house rules are deterministic Java, so they
 * must be tested WITHOUT the model. Use a fixed Clock for rules that depend on time:
 *
 * <pre>{@code
 * RenewalService s = new RenewalService(books, new RenewalRules(2, 14), at("2026-11-02T15:00"));
 * LocalDate due = s.renew("S2026001", "LB-00412");
 * assertEquals(LocalDate.parse("2026-11-16"), due);
 * }</pre>
 * Minimum for Part B: one test per house rule (K1-K4 / M1-M4 / ...), including the boundary values.
 */
class MyServiceTest {

    static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");

    static Clock at(String isoLocal) {
        return Clock.fixed(LocalDateTime.parse(isoLocal).atZone(KL).toInstant(), KL);
    }

    @Test
    void fixedClockExample() {
        assertEquals(15, LocalDateTime.now(at("2026-11-02T15:00")).getHour());
    }
}
