package my.edu.taylors.ap.scenario.rooms;

import java.time.LocalDate;
import java.time.LocalTime;

/** Row of runtime/bookings.json. date = yyyy-MM-dd, start/end = HH:mm. */
public record Booking(String bookingId, String roomCode, String date, String start, String end, int groupSize, String bookedBy) {

    public boolean overlaps(String room, LocalDate d, LocalTime s, LocalTime e) {
        return roomCode.equalsIgnoreCase(room) && LocalDate.parse(date).equals(d)
                && s.isBefore(LocalTime.parse(end)) && LocalTime.parse(start).isBefore(e);
    }
}
