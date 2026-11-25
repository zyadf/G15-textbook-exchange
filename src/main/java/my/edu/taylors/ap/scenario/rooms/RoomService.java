package my.edu.taylors.ap.scenario.rooms;

import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.rooms.RoomExceptions.BlockedDayException;
import my.edu.taylors.ap.scenario.rooms.RoomExceptions.BookingConflictException;
import my.edu.taylors.ap.scenario.rooms.RoomExceptions.CapacityException;
import my.edu.taylors.ap.scenario.rooms.RoomExceptions.InvalidSlotException;
import my.edu.taylors.ap.scenario.rooms.RoomExceptions.UnknownRoomException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Deterministic booking rules R1-R4. */
public class RoomService {

    private final List<Room> rooms;
    private final RoomRules rules;
    private final Clock clock;

    public RoomService(List<Room> rooms, RoomRules rules, Clock clock) {
        this.rooms = List.copyOf(rooms);
        this.rules = rules;
        this.clock = clock;
    }

    public List<Room> rooms() {
        return rooms;
    }

    public Room room(String code) throws UnknownRoomException {
        return rooms.stream().filter(r -> r.code().equalsIgnoreCase(code == null ? "" : code.trim()))
                .findFirst().orElseThrow(() -> new UnknownRoomException(code));
    }

    /** Validates R1-R4 and returns the booking that WOULD be created. */
    public Booking validate(String userId, String roomCode, LocalDate date, LocalTime start, int durationMinutes,
                            int groupSize, List<Booking> existing, String newId) throws RuleViolationException {
        Room room = room(roomCode);
        if (room.endsInOddDigit() && date.getDayOfWeek() == rules.blockedDay()) {
            throw new BlockedDayException(room.code(), rules.blockedDay().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        if (start.getMinute() % 30 != 0 || start.getSecond() != 0) {
            throw new InvalidSlotException("Bookings must start on the hour or half hour.");
        }
        if (durationMinutes < 30 || durationMinutes > rules.maxHours() * 60 || durationMinutes % 30 != 0) {
            throw new InvalidSlotException("Duration must be 30 minutes to " + rules.maxHours() + " hours, in 30-minute steps.");
        }
        int startMin = start.getHour() * 60 + start.getMinute();
        if (startMin < RoomRules.OPEN_MINUTE || startMin + durationMinutes > RoomRules.CLOSE_MINUTE) {
            throw new InvalidSlotException("Bookings must be between 08:00 and 22:00.");
        }
        LocalDateTime startAt = LocalDateTime.of(date, start);
        if (startAt.isBefore(LocalDateTime.now(clock).plusHours(rules.minAdvanceHours()))) {
            throw new InvalidSlotException("Bookings must be made at least " + rules.minAdvanceHours() + " hours in advance.");
        }
        int max = rules.maxGroup(room.capacity());
        if (groupSize < 1 || groupSize > max) {
            throw new CapacityException(room.code(), max, groupSize);
        }
        LocalTime end = start.plusMinutes(durationMinutes);
        Optional<Booking> clash = existing.stream().filter(b -> b.overlaps(room.code(), date, start, end)).findFirst();
        if (clash.isPresent()) {
            throw new BookingConflictException(room.code(), "from " + clash.get().start() + " to " + clash.get().end());
        }
        return new Booking(newId, room.code(), date.toString(), start.toString(), end.toString(), groupSize, userId);
    }
}
