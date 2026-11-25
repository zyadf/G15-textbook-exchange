package my.edu.taylors.ap.scenario.rooms;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.DateTimes;
import my.edu.taylors.ap.scenario.common.RuleViolationException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Room booking tools. Bookings are made for the current user only (@ToolMemoryId). */
public class RoomTools {

    private final RoomService service;
    private final JsonStore<Booking> bookings;

    public RoomTools(RoomService service, JsonStore<Booking> bookings) {
        this.service = service;
        this.bookings = bookings;
    }

    @Tool("Find rooms that fit a group. Returns room codes, type, usable capacity and projector availability.")
    public String findRooms(@P("number of people") int groupSize, @P("true if a projector is required") boolean needProjector) {
        return service.rooms().stream()
                .filter(r -> !needProjector || r.projector())
                .sorted(Comparator.comparingInt(Room::capacity))
                .map(r -> r.code() + " " + r.type() + " capacity " + r.capacity() + (r.projector() ? " projector" : ""))
                .collect(Collectors.joining("\n"));
    }

    @Tool("List existing bookings of a room on a date, so you can suggest free slots.")
    public String roomSchedule(@P("room code, e.g. C7.03") String roomCode, @P("date yyyy-MM-dd") String date)
            throws RuleViolationException, DataAccessException {
        Room room = service.room(roomCode);
        LocalDate d = DateTimes.parseDate(date);
        List<Booking> day = bookings.findWhere(b -> b.roomCode().equalsIgnoreCase(room.code()) && b.date().equals(d.toString()));
        if (day.isEmpty()) {
            return room.code() + " has no bookings on " + d + ".";
        }
        return day.stream().map(b -> b.start() + "-" + b.end() + " booked").collect(Collectors.joining("\n"));
    }

    @Tool("Book a room for the current user. Returns the booking ID (BK-nnnn) only when the booking succeeds.")
    public String bookRoom(@ToolMemoryId String userId,
                           @P("room code, e.g. C7.03") String roomCode,
                           @P("date yyyy-MM-dd") String date,
                           @P("start time HH:mm (24-hour)") String startTime,
                           @P("duration in minutes") int durationMinutes,
                           @P("number of people") int groupSize) throws RuleViolationException, DataAccessException {
        List<Booking> all = bookings.findAll();
        Booking b = service.validate(userId, roomCode, DateTimes.parseDate(date), DateTimes.parseTime(startTime),
                durationMinutes, groupSize, all, String.format("BK-%04d", all.size() + 1));
        bookings.append(b);
        return "Booking " + b.bookingId() + " confirmed: " + b.roomCode() + " on " + b.date() + " " + b.start() + "-" + b.end() + ".";
    }

    @Tool("List the current user's upcoming bookings.")
    public String myBookings(@ToolMemoryId String userId) throws DataAccessException {
        List<Booking> mine = bookings.findWhere(b -> b.bookedBy().equalsIgnoreCase(userId));
        return mine.isEmpty() ? "You have no bookings." : mine.stream()
                .map(b -> b.bookingId() + " " + b.roomCode() + " " + b.date() + " " + b.start() + "-" + b.end())
                .collect(Collectors.joining("\n"));
    }
}
