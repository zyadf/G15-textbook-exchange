package my.edu.taylors.ap.scenario.grooming;

import java.time.LocalDate;
import java.time.LocalTime;

/** Row of runtime/appointments.json. */
public record Appointment(String appointmentId, String petId, String serviceCode, String groomer,
                          String date, String start, String end, long priceSen, String ownerId) {

    public boolean overlaps(String groomerName, LocalDate d, LocalTime s, LocalTime e) {
        return groomer.equalsIgnoreCase(groomerName) && LocalDate.parse(date).equals(d)
                && s.isBefore(LocalTime.parse(end)) && LocalTime.parse(start).isBefore(e);
    }
}
