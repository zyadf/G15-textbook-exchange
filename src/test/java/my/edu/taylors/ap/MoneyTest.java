package my.edu.taylors.ap;

import my.edu.taylors.ap.scenario.common.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {

    @Test
    void roundsToNearestFiveSen() {
        assertEquals(1000, Money.roundToNearest5Sen(1001));
        assertEquals(1000, Money.roundToNearest5Sen(1002));
        assertEquals(1005, Money.roundToNearest5Sen(1003));
        assertEquals(1005, Money.roundToNearest5Sen(1004));
        assertEquals(1005, Money.roundToNearest5Sen(1006));
        assertEquals(1005, Money.roundToNearest5Sen(1007));
        assertEquals(1010, Money.roundToNearest5Sen(1008));
        assertEquals(1010, Money.roundToNearest5Sen(1009));
        assertEquals(1010, Money.roundToNearest5Sen(1010));
    }

    @Test
    void percentRoundsHalfUp() {
        assertEquals(140, Money.percentOf(1999, 7));   // 139.93
        assertEquals(50, Money.percentOf(1000, 5));
        assertEquals(1, Money.percentOf(10, 5));       // 0.5 -> 1
    }

    @Test
    void formatsRinggit() {
        assertEquals("RM12.05", Money.rm(1205));
        assertEquals("RM0.00", Money.rm(0));
    }
}
