package my.edu.taylors.ap.scenario.rooms;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.trace.TraceLogger;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OUTPUT guardrail for R5 "no phantom bookings": if the answer claims a booking is
 * confirmed/booked/reserved, every BK-nnnn it cites must exist in bookings.json, and at least one must be cited.
 */
public class PhantomBookingGuardrail implements OutputGuardrail {

    static final Pattern CLAIM = Pattern.compile("\\b(confirmed|is booked|have booked|i've booked|reserved|booking is done)\\b",
            Pattern.CASE_INSENSITIVE);
    static final Pattern ID = Pattern.compile("BK-\\d{4}");

    private final JsonStore<Booking> bookings;
    private final TraceLogger trace;

    public PhantomBookingGuardrail(JsonStore<Booking> bookings, TraceLogger trace) {
        this.bookings = bookings;
        this.trace = trace;
    }

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text() == null ? "" : responseFromLLM.text();
        if (!CLAIM.matcher(text).find()) {
            return success();
        }
        Matcher m = ID.matcher(text);
        boolean cited = false;
        try {
            while (m.find()) {
                cited = true;
                String id = m.group();
                if (bookings.findWhere(b -> b.bookingId().equals(id)).isEmpty()) {
                    return phantom("cited unknown " + id);
                }
            }
        } catch (DataAccessException e) {
            return fatal("Cannot verify bookings: " + e.getMessage());
        }
        return cited ? success() : phantom("claim without booking ID");
    }

    private OutputGuardrailResult phantom(String reason) {
        trace.log("guardrail.output", Map.of("rule", "R5", "result", "reprompt", "reason", reason));
        return reprompt("R5: " + reason, "Do not claim a booking is confirmed unless the bookRoom tool returned its ID. "
                + "Call bookRoom if the user wants to book, otherwise say it is not booked yet.");
    }
}
