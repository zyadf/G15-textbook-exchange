package my.edu.taylors.ap.scenario;

import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Rules fixed to T=RM20.00, N=4, H=09:00, S=6% so the expected values are easy to check by hand. */
class KopitiamServiceTest {

    static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    static final KopitiamRules RULES = new KopitiamRules(2000, 4, 540, 6);
    static final List<MenuItem> MENU = List.of(
            new MenuItem("D01", "Teh Tarik", "DRINK", 280, List.of("dairy"), true),
            new MenuItem("D02", "Kopi O", "DRINK", 220, List.of(), true),
            new MenuItem("F02", "Mee Goreng Mamak", "FOOD", 790, List.of("gluten"), true),
            new MenuItem("F04", "Char Kuey Teow", "FOOD", 950, List.of("shellfish"), true),
            new MenuItem("K01", "Kaya Toast", "SNACK", 380, List.of("gluten"), true),
            new MenuItem("S02", "Kuih Lapis", "SNACK", 150, List.of(), false));

    static KopitiamService at(String isoLocal) {
        return new KopitiamService(MENU, RULES, Clock.fixed(LocalDateTime.parse(isoLocal).atZone(KL).toInstant(), KL));
    }

    @Test
    void dineInServiceChargeAndRounding() throws RuleViolationException {
        // Monday 12:00. 2 x D01 (5.60) + F02 (7.90) = 13.50; 6% = 0.81 -> 14.31 -> rounds to 14.30
        Quote q = at("2026-11-02T12:00").quote(KopitiamService.parseItems("D01x2,F02x1"), true);
        assertEquals(1350, q.subtotalSen());
        assertEquals(81, q.serviceChargeSen());
        assertEquals(1431, q.unroundedSen());
        assertEquals(1430, q.totalSen());
        assertFalse(q.freeToastApplied());
    }

    @Test
    void takeawayPaysContainersNotServiceCharge() throws RuleViolationException {
        // 3 drinks -> 0.60 containers; 2.80*2 + 2.20 = 7.80 -> 8.40
        Quote q = at("2026-11-02T12:00").quote(KopitiamService.parseItems("D01x2, D02x1"), false);
        assertEquals(0, q.serviceChargeSen());
        assertEquals(60, q.containerFeeSen());
        assertEquals(840, q.totalSen());
    }

    @Test
    void afternoonTreatAtThreshold() throws RuleViolationException {
        // 15:00 exactly, subtotal 2 x 9.50 + 1 x 2.20 = 21.20 >= 20.00
        Quote q = at("2026-11-02T15:00").quote(KopitiamService.parseItems("F04x2,D02x1"), false);
        assertTrue(q.freeToastApplied());
        assertEquals(2120, q.subtotalSen());
    }

    @Test
    void noTreatAtSixPm() throws RuleViolationException {
        Quote q = at("2026-11-02T18:00").quote(KopitiamService.parseItems("F04x3"), true);
        assertFalse(q.freeToastApplied());
    }

    @Test
    void rushHourCapOnWeekdayMorning() {
        assertThrows(KopitiamExceptions.RushHourLimitException.class,
                () -> at("2026-11-02T08:59").quote(KopitiamService.parseItems("D01x5"), true));
    }

    @Test
    void noRushHourCapOnSaturdayOrAfterH() throws RuleViolationException {
        at("2026-11-07T08:00").quote(KopitiamService.parseItems("D01x5"), true);   // Saturday
        at("2026-11-02T09:00").quote(KopitiamService.parseItems("D01x5"), true);   // Monday at H
    }

    @Test
    void duplicateLinesAreMergedBeforeTheCap() {
        assertThrows(KopitiamExceptions.RushHourLimitException.class,
                () -> at("2026-11-03T07:30").quote(KopitiamService.parseItems("D01x3,D01x2"), false));
    }

    @Test
    void badInputRaisesSpecificExceptions() {
        KopitiamService s = at("2026-11-02T12:00");
        assertThrows(KopitiamExceptions.UnknownMenuItemException.class, () -> s.quote(KopitiamService.parseItems("Z99x1"), true));
        assertThrows(KopitiamExceptions.ItemUnavailableException.class, () -> s.quote(KopitiamService.parseItems("S02x1"), true));
        assertThrows(KopitiamExceptions.InvalidQuantityException.class, () -> s.quote(KopitiamService.parseItems("D02x0"), true));
        assertThrows(KopitiamExceptions.OrderFormatException.class, () -> KopitiamService.parseItems("two teh tarik"));
    }
}
