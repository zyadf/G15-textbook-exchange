package my.edu.taylors.ap.scenario.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Parses the ISO dates/times that tools receive from the model, with a clear error for the model. */
public final class DateTimes {

    private DateTimes() { }

    public static LocalDate parseDate(String s) throws RuleViolationException {
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RuleViolationException("FORMAT", "Cannot read date '" + s + "'. Use yyyy-MM-dd.");
        }
    }

    public static LocalTime parseTime(String s) throws RuleViolationException {
        try {
            return LocalTime.parse(s.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RuleViolationException("FORMAT", "Cannot read time '" + s + "'. Use HH:mm (24-hour).");
        }
    }
}
