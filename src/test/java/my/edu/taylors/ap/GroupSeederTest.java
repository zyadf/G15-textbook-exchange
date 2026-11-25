package my.edu.taylors.ap;

import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.GroupSeeder;
import my.edu.taylors.ap.config.Scenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupSeederTest {

    @Test
    void sameGroupAlwaysGetsSameProfile() {
        assertEquals(GroupSeeder.profileFor(7), GroupSeeder.profileFor(7));
    }

    @Test
    void scenariosRotateByGroupNumber() {
        assertEquals(Scenario.KOPITIAM, GroupSeeder.profileFor(1).scenario());
        assertEquals(Scenario.MAKERSPACE, GroupSeeder.profileFor(2).scenario());
        assertEquals(Scenario.TEXTBOOKS, GroupSeeder.profileFor(5).scenario());
        assertEquals(Scenario.KOPITIAM, GroupSeeder.profileFor(6).scenario());
    }

    @Test
    void parametersStayInDocumentedRanges() {
        for (int g = 1; g <= 99; g++) {
            GroupProfile p = GroupSeeder.profileFor(g);
            switch (p.scenario()) {
                case KOPITIAM -> {
                    assertTrue(p.param("T") >= 1800 && p.param("T") <= 3590);
                    assertTrue(p.param("N") >= 3 && p.param("N") <= 6);
                    assertTrue(p.param("S") >= 5 && p.param("S") <= 8);
                }
                case MAKERSPACE -> assertTrue(p.param("B") >= 24 && p.param("B") <= 72);
                case ROOMS -> assertTrue(p.param("W") >= 1 && p.param("W") <= 5);
                case GROOMING -> assertEquals(p.param("LF") + 3, p.param("XF"));
                case TEXTBOOKS -> assertTrue(p.param("P") >= 3 && p.param("P") <= 7);
            }
            assertEquals(4, p.token().length());
        }
    }

    @Test
    void rejectsInvalidGroupNumbers() {
        assertThrows(IllegalArgumentException.class, () -> GroupSeeder.profileFor(0));
        assertThrows(IllegalArgumentException.class, () -> GroupSeeder.profileFor(100));
    }
}
