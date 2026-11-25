package my.edu.taylors.ap.scenario.rooms;

import my.edu.taylors.ap.scenario.common.RuleViolationException;

/** Custom checked exceptions for the room booking scenario. */
public final class RoomExceptions {

    private RoomExceptions() { }

    public static class UnknownRoomException extends RuleViolationException {
        public UnknownRoomException(String code) {
            super("ROOM", "No room with code '" + code + "'. Use findRooms first.");
        }
    }

    public static class BlockedDayException extends RuleViolationException {
        public BlockedDayException(String room, String day) {
            super("R1", "Room " + room + " ends in an odd digit and cannot be booked on " + day + ".");
        }
    }

    public static class InvalidSlotException extends RuleViolationException {
        public InvalidSlotException(String detail) {
            super("R2", detail);
        }
    }

    public static class CapacityException extends RuleViolationException {
        public CapacityException(String room, int max, int requested) {
            super("R3", "Room " + room + " allows at most " + max + " people (requested " + requested + ").");
        }
    }

    public static class BookingConflictException extends RuleViolationException {
        public BookingConflictException(String room, String detail) {
            super("R4", "Room " + room + " is already booked " + detail + ".");
        }
    }
}
