package my.edu.taylors.ap.scenario.rooms;

import my.edu.taylors.ap.config.GroupProfile;

import java.time.DayOfWeek;

/**
 * @param blockedDay        R1 day W on which odd-digit rooms are blocked
 * @param maxHours          R2 maximum length L
 * @param capacityPercent   R3 usable percentage P
 * @param minAdvanceHours   R2 minimum advance notice A
 */
public record RoomRules(DayOfWeek blockedDay, int maxHours, int capacityPercent, int minAdvanceHours) {

    public static final int OPEN_MINUTE = 8 * 60;
    public static final int CLOSE_MINUTE = 22 * 60;

    public static RoomRules from(GroupProfile g) {
        return new RoomRules(DayOfWeek.of(g.param("W")), g.param("L"), g.param("P"), g.param("A"));
    }

    /** R3: largest allowed group for a room. */
    public int maxGroup(int capacity) {
        return capacity * capacityPercent / 100;
    }
}
