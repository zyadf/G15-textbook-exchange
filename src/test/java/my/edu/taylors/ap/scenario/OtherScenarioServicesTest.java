package my.edu.taylors.ap.scenario;

import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.grooming.*;
import my.edu.taylors.ap.scenario.makerspace.*;
import my.edu.taylors.ap.scenario.rooms.*;
import my.edu.taylors.ap.scenario.textbooks.Book;
import my.edu.taylors.ap.scenario.textbooks.TextbookExceptions;
import my.edu.taylors.ap.scenario.textbooks.TextbookRules;
import my.edu.taylors.ap.scenario.textbooks.TextbookService;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OtherScenarioServicesTest {

    static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");

    static Clock at(String isoLocal) {
        return Clock.fixed(LocalDateTime.parse(isoLocal).atZone(KL).toInstant(), KL);
    }

    // ---------------- Makerspace: B=48, C=2, F=40 ----------------
    static final MakerspaceRules M = new MakerspaceRules(48, 2, 40);
    static final List<Equipment> EQ = List.of(
            new Equipment("EQ-SOL-01", "Soldering station", "SOLDERING", 1),
            new Equipment("EQ-PRN-01", "3D printer", "PRINTER3D", 2),
            new Equipment("EQ-ELE-01", "Arduino kit", "ELECTRONICS", 5));
    static final List<Member> MEMBERS = List.of(
            new Member("MKR-0113", "Aina", true), new Member("MKR-0114", "Daniel", false));

    @Test
    void makerspaceLoanPeriods() throws RuleViolationException {
        MakerspaceService s = new MakerspaceService(EQ, MEMBERS, M, at("2026-11-02T10:00"));
        assertEquals("2026-11-04T22:00", s.prepareLoan("MKR-0113", "EQ-ELE-01", List.of(), "x").due()); // odd: 48+12
        assertEquals("2026-11-04T10:00", s.prepareLoan("MKR-0114", "EQ-ELE-01", List.of(), "x").due()); // even: 48
        assertEquals("2026-11-02T18:00", s.prepareLoan("MKR-0113", "EQ-PRN-01", List.of(), "x").due()); // printer: 8
    }

    @Test
    void makerspaceRules() {
        MakerspaceService s = new MakerspaceService(EQ, MEMBERS, M, at("2026-11-02T10:00"));
        assertThrows(MakerspaceExceptions.SafetyQuizRequiredException.class,
                () -> s.prepareLoan("MKR-0114", "EQ-SOL-01", List.of(), "x"));
        List<Loan> two = List.of(new Loan("L1", "MKR-0113", "EQ-ELE-01", "", "2026-11-03T10:00", null),
                new Loan("L2", "MKR-0113", "EQ-ELE-01", "", "2026-11-03T10:00", null));
        assertThrows(MakerspaceExceptions.LoanLimitExceededException.class,
                () -> s.prepareLoan("MKR-0113", "EQ-PRN-01", two, "x"));
        List<Loan> solderOut = List.of(new Loan("L3", "MKR-0114", "EQ-SOL-01", "", "2026-11-03T10:00", null));
        assertThrows(MakerspaceExceptions.OutOfStockException.class,
                () -> s.prepareLoan("MKR-0113", "EQ-SOL-01", solderOut, "x"));
    }

    @Test
    void makerspaceLateFee() {
        MakerspaceService s = new MakerspaceService(EQ, MEMBERS, M, at("2026-11-02T10:00"));
        Loan l = new Loan("L1", "MKR-0113", "EQ-ELE-01", "2026-11-01T10:00", "2026-11-02T10:00", null);
        assertEquals(0, s.lateFeeSen(l, LocalDateTime.parse("2026-11-02T10:00")));
        assertEquals(40, s.lateFeeSen(l, LocalDateTime.parse("2026-11-02T10:01")));    // 1 started hour
        assertEquals(80, s.lateFeeSen(l, LocalDateTime.parse("2026-11-02T11:01")));    // 2 started hours
        assertEquals(3000, s.lateFeeSen(l, LocalDateTime.parse("2026-11-09T10:00")));  // capped
    }

    // ---------------- Rooms: W=WEDNESDAY, L=3, P=80, A=6 ----------------
    static final RoomRules R = new RoomRules(DayOfWeek.WEDNESDAY, 3, 80, 6);
    static final List<Room> ROOMS = List.of(new Room("C7.03", "LAB", 30, true), new Room("D5.04", "DISCUSSION", 8, false));

    @Test
    void roomRules() throws RuleViolationException {
        RoomService s = new RoomService(ROOMS, R, at("2026-11-02T08:00"));   // Monday 8 am
        LocalDate wed = LocalDate.parse("2026-11-04");
        assertThrows(RoomExceptions.BlockedDayException.class,
                () -> s.validate("u", "C7.03", wed, LocalTime.parse("10:00"), 60, 10, List.of(), "x"));
        s.validate("u", "D5.04", wed, LocalTime.parse("10:00"), 60, 6, List.of(), "x");          // even digit is fine
        assertThrows(RoomExceptions.CapacityException.class,                                      // 80% of 8 = 6
                () -> s.validate("u", "D5.04", wed, LocalTime.parse("10:00"), 60, 7, List.of(), "x"));
        assertThrows(RoomExceptions.InvalidSlotException.class,                                   // 13:00 < now + 6h
                () -> s.validate("u", "D5.04", LocalDate.parse("2026-11-02"), LocalTime.parse("13:00"), 60, 2, List.of(), "x"));
        assertThrows(RoomExceptions.InvalidSlotException.class,                                   // > L hours
                () -> s.validate("u", "D5.04", wed, LocalTime.parse("10:00"), 210, 2, List.of(), "x"));
        assertThrows(RoomExceptions.InvalidSlotException.class,                                   // ends after 22:00
                () -> s.validate("u", "D5.04", wed, LocalTime.parse("21:30"), 60, 2, List.of(), "x"));
        List<Booking> existing = List.of(new Booking("BK-0001", "D5.04", "2026-11-04", "11:00", "12:00", 4, "v"));
        assertThrows(RoomExceptions.BookingConflictException.class,
                () -> s.validate("u", "D5.04", wed, LocalTime.parse("10:30"), 60, 2, existing, "x"));
        s.validate("u", "D5.04", wed, LocalTime.parse("10:00"), 60, 2, existing, "x");          // touching is fine
    }

    // ---------------- Grooming: LF=14, XF=17, X=RM20, D=10% ----------------
    static GroomingService grooming() {
        return new GroomingService(
                List.of(new GroomService("FULL", "Full", 90, 8500), new GroomService("NAIL", "Nail", 15, 1500)),
                List.of(new Breed("Shih Tzu", "S"), new Breed("Corgi", "M"), new Breed("Golden Retriever", "L"), new Breed("Great Dane", "XL")),
                List.of(new Groomer("Mei", "M"), new Groomer("Kenji", "L"), new Groomer("Farah", "XL")),
                new GroomingRules(14, 17, 2000, 10));
    }

    @Test
    void groomingEstimates() throws RuleViolationException {
        GroomingService s = grooming();
        Estimate l = s.estimate("Golden Retriever", "FULL", true);   // 90 x 1.4 = 126 -> 135 min
        assertEquals(135, l.minutes());
        assertEquals(850, l.discountSen());
        assertEquals(8500 + 2000 - 850, l.totalSen());
        Estimate xl = s.estimate("great dane", "NAIL", false);       // 15 x 1.7 = 25.5 -> 30 min
        assertEquals(30, xl.minutes());
        assertEquals(1500 + 4000, xl.totalSen());
        assertEquals(30, s.estimate("Corgi", "NAIL", false).minutes()); // 15 x 1.2 = 18 -> 30
    }

    @Test
    void groomingScheduling() throws RuleViolationException {
        GroomingService s = grooming();
        LocalDate sat = LocalDate.parse("2026-11-07");
        assertEquals("Kenji", s.assignGroomer("L", sat, LocalTime.parse("10:00"), 135, List.of()).name());
        List<Appointment> kenjiBusy = List.of(new Appointment("GR-1", "P", "FULL", "Kenji", "2026-11-07", "09:30", "11:00", 0, "o"));
        assertEquals("Farah", s.assignGroomer("L", sat, LocalTime.parse("10:00"), 135, kenjiBusy).name());
        assertThrows(RuleViolationException.class, () -> s.assignGroomer("XL", sat, LocalTime.parse("10:00"), 30, List.of()));
        assertThrows(RuleViolationException.class, () -> s.assignGroomer("S", sat, LocalTime.parse("17:30"), 45, List.of()));
    }

    // ---------------- Textbooks: Y=10, Z=15, F=RM8, P=5 ----------------
    static final TextbookRules TB = new TextbookRules(10, 15, 800, 5);

    @Test
    void textbookPricing() throws RuleViolationException {
        TextbookService s = new TextbookService(List.of(), TB, at("2026-11-02T10:00"));
        Book current = new Book("9780000000101", "Intro Java", 12, 2021, 25900, null);
        // 259.00 x 0.9^5 x 0.85 = 130.00 (0.9^5 = 0.59049 -> 152.937; x0.85 = 129.996) -> floor 0.50 -> 129.50
        assertEquals(12950, s.suggestedSen(current, "B"));
        Book old = new Book("9780000000095", "Intro Java", 11, 2018, 23900, "9780000000101");
        // 239 x 0.9^8 x 0.40 x 0.85 = 34.98... -> 34.50
        assertEquals(3450, s.suggestedSen(old, "D"));
        Book cheap = new Book("1", "Cheap", 1, 2000, 2000, null);
        assertEquals(800, s.suggestedSen(cheap, "C"));                 // floor price
        assertEquals(15540, s.maxAskingSen(12950));                      // +20%
        assertEquals(10500, s.buyerTotalSen(10000));                     // +5%
        assertThrows(TextbookExceptions.OverpricedListingException.class, () -> s.validateListing(current, "B", 15541, ""));
        assertThrows(TextbookExceptions.MissingDisclosureException.class, () -> s.validateListing(old, "D", 3000, " "));
        assertEquals(4550, TextbookService.parseRinggit("RM45.50"));
    }
}
