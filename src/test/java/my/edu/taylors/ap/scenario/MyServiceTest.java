// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario;

import my.edu.taylors.ap.config.GroupSeeder;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.mine.Book;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions;
import my.edu.taylors.ap.scenario.mine.ExchangeRules;
import my.edu.taylors.ap.scenario.mine.ExchangeService;
import my.edu.taylors.ap.scenario.mine.PriceSuggestion;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * House rules T1-T4 for GROUP 15 with the real seeded values (Y=9%, Z=10%, F=RM6.00, P=5%).
 * No language model is involved: the rules are plain Java, so every expected value below can
 * be checked with a calculator (the working is in the comments).
 */
class MyServiceTest {

    static final String QUILLMARK = "generated";

    static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    static final ExchangeRules G15 = ExchangeRules.from(GroupSeeder.profileFor(15));

    // Two real rows from books.json
    static final Book JAVA_12 = new Book("9780000000101", "Introduction to Java Programming and Data Structures", 12, 2021, 25900, null);
    static final Book REF_12_OLD = new Book("9780000000194", "Java: The Complete Reference", 12, 2021, 19900, "9780000000200");

    static Clock at(String isoLocal) {
        return Clock.fixed(LocalDateTime.parse(isoLocal).atZone(KL).toInstant(), KL);
    }

    static ExchangeService service(String isoLocal, Book... books) {
        return new ExchangeService(List.of(books), G15, at(isoLocal));
    }

    @Test
    void seededValuesMatchTheHouseRulesCard() {
        assertEquals(new ExchangeRules(9, 10, 600, 5), G15);
    }

    // ------------------------------------------------------------------ T1

    @Test
    void t1CurrentEditionGradeB() throws RuleViolationException {
        // age 2026-2021 = 5; 0.91^5 = 0.6240321451
        // 259.00 x 0.6240321451 x 0.85 = 137.3806... -> round DOWN to RM0.50 -> RM137.00
        PriceSuggestion s = service("2026-11-02T10:00").suggest(JAVA_12, "B");
        assertEquals(5, s.age());
        assertEquals(13700, s.suggestedSen());
        assertFalse(s.floorApplied());
    }

    @Test
    void t1SupersededEditionJustBelowAFiftySenStep() throws RuleViolationException {
        // 199.00 x 0.91^5 x 0.85 x 0.90 = 94.99953... -> rounds DOWN to RM94.50, NOT up to RM95.00
        PriceSuggestion s = service("2026-11-02T10:00").suggest(REF_12_OLD, "B");
        assertEquals(9450, s.suggestedSen());
    }

    @Test
    void t1AllGradesForOneBook() throws RuleViolationException {
        ExchangeService s = service("2026-11-02T10:00");
        assertEquals(16150, s.suggest(JAVA_12, "A").suggestedSen());   // 161.62 -> 161.50
        assertEquals(10500, s.suggest(JAVA_12, "C").suggestedSen());   // 105.06 -> 105.00
        assertEquals(6450, s.suggest(JAVA_12, "D").suggestedSen());    //  64.65 ->  64.50
    }

    @Test
    void t1FloorPrice() throws RuleViolationException {
        // age 0, grade D: 15.00 x 0.40 = 6.00 exactly -> RM6.00 by the formula (floor NOT needed)
        Book exact = new Book("1", "Exactly at floor", 1, 2026, 1500, null);
        PriceSuggestion atFloor = service("2026-11-02T10:00").suggest(exact, "D");
        assertEquals(600, atFloor.suggestedSen());
        assertFalse(atFloor.floorApplied());
        // 14.99 x 0.40 = 5.996 -> RM5.50 -> below RM6.00 -> floor RM6.00
        Book below = new Book("2", "One sen below", 1, 2026, 1499, null);
        PriceSuggestion floored = service("2026-11-02T10:00").suggest(below, "D");
        assertEquals(600, floored.suggestedSen());
        assertTrue(floored.floorApplied());
    }

    @Test
    void t1AgeChangesOnNewYearsDay() throws RuleViolationException {
        // 31 Dec 2026: age 5 -> RM161.50.  1 Jan 2027: age 6 -> 259 x 0.91^6 = 147.078 -> RM147.00
        assertEquals(16150, service("2026-12-31T23:59").suggest(JAVA_12, "A").suggestedSen());
        assertEquals(14700, service("2027-01-01T00:00").suggest(JAVA_12, "A").suggestedSen());
    }

    @Test
    void t1FuturePublicationYearCountsAsAgeZero() throws RuleViolationException {
        Book future = new Book("3", "Next year's book", 1, 2027, 10000, null);
        assertEquals(0, service("2026-11-02T10:00").suggest(future, "A").age());
        assertEquals(10000, service("2026-11-02T10:00").suggest(future, "A").suggestedSen());
    }

    @Test
    void t1GradeIsValidated() throws RuleViolationException {
        assertEquals("B", ExchangeService.normaliseGrade(" b "));
        assertEquals("C", ExchangeService.normaliseGrade("grade C"));
        assertThrows(ExchangeExceptions.InvalidConditionException.class, () -> ExchangeService.normaliseGrade("E"));
        assertThrows(ExchangeExceptions.InvalidConditionException.class, () -> ExchangeService.normaliseGrade(null));
    }

    // ------------------------------------------------------------------ T2

    @Test
    void t2AtMostTwentyPercentAboveSuggested() throws RuleViolationException {
        ExchangeService s = service("2026-11-02T10:00");
        // suggested RM137.00 x 1.20 = RM164.40
        assertEquals(16440, s.suggest(JAVA_12, "B").maxAskingSen());
        s.validateListing(JAVA_12, "B", 16440, "");                     // exactly at the limit: allowed
        assertThrows(ExchangeExceptions.OverpricedListingException.class,
                () -> s.validateListing(JAVA_12, "B", 16441, ""));      // one sen above: rejected
    }

    @Test
    void t2RejectsZeroOrNegativePrice() {
        ExchangeService s = service("2026-11-02T10:00");
        assertThrows(ExchangeExceptions.InvalidPriceException.class, () -> s.validateListing(JAVA_12, "B", 0, ""));
        assertThrows(ExchangeExceptions.InvalidPriceException.class, () -> s.validateListing(JAVA_12, "B", -100, ""));
    }

    // ------------------------------------------------------------------ T3

    @Test
    void t3BuyerFeeHalfUp() {
        ExchangeService s = service("2026-11-02T10:00");
        assertEquals(14700, s.buyerTotalSen(14000));   // 140.00 + 5% (7.00)
        assertEquals(1061, s.buyerTotalSen(1010));     // 5% of 10.10 = 0.505 -> half-up 0.51
        assertEquals(1059, s.buyerTotalSen(1009));     // 5% of 10.09 = 0.5045 -> 0.50
    }

    // ------------------------------------------------------------------ T4

    @Test
    void t4GradeDNeedsDefects() throws RuleViolationException {
        ExchangeService s = service("2026-11-02T10:00");
        assertThrows(ExchangeExceptions.MissingDefectsException.class, () -> s.validateListing(JAVA_12, "D", 5000, ""));
        assertThrows(ExchangeExceptions.MissingDefectsException.class, () -> s.validateListing(JAVA_12, "D", 5000, "   "));
        assertThrows(ExchangeExceptions.MissingDefectsException.class, () -> s.validateListing(JAVA_12, "D", 5000, null));
        s.validateListing(JAVA_12, "D", 5000, "cover detached");     // described: allowed
        s.validateListing(JAVA_12, "C", 5000, "");                    // only grade D needs it
    }

    // ------------------------------------------------------------------ input handling

    @Test
    void parsesRinggitSafely() throws RuleViolationException {
        assertEquals(4550, ExchangeService.parseRinggit("RM45.50"));
        assertEquals(4500, ExchangeService.parseRinggit("45"));
        assertEquals(4550, ExchangeService.parseRinggit(" rm 45.5 "));
        for (String bad : new String[]{"fifty", "-5", "0", "45.555", "", null}) {
            assertThrows(ExchangeExceptions.InvalidPriceException.class, () -> ExchangeService.parseRinggit(bad), bad);
        }
    }

    @Test
    void unknownIsbnIsRejected() throws RuleViolationException {
        ExchangeService s = service("2026-11-02T10:00", JAVA_12);
        assertEquals(JAVA_12, s.book("978-0000000101"));
        assertThrows(ExchangeExceptions.UnknownBookException.class, () -> s.book("9789999999999"));
    }
}
