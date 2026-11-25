package my.edu.taylors.ap.scenario.rooms;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.guardrail.ScopeInputGuardrail;
import my.edu.taylors.ap.scenario.common.DateTimes;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.scenario.common.StructuredResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Scenario 3 wiring. */
public class RoomsModule implements ScenarioModule {

    interface BookingExtractor {
        @UserMessage("""
                Today is {{today}}. Extract the room booking request from the message.
                Message: {{text}}""")
        BookingRequest extract(@V("text") String text, @V("today") String today);
    }

    private final RoomService service;
    private final RoomTools tools;
    private final JsonStore<Booking> bookings;
    private final BookingExtractor extractor;
    private final TraceLogger trace;
    private final Clock clock;
    private final Path dataDir;

    public RoomsModule(GroupProfile profile, Path dataDir, Path runtimeDir, ChatModel model,
                       TraceLogger trace, Clock clock) throws DataAccessException {
        List<Room> rooms = JsonStore.of(dataDir.resolve("rooms.json"), Room.class).findAll();
        this.service = new RoomService(rooms, RoomRules.from(profile), clock);
        this.bookings = JsonStore.seeded(dataDir.resolve("bookings.json"), runtimeDir.resolve("bookings.json"), Booking.class);
        this.tools = new RoomTools(service, bookings);
        this.extractor = AiServices.builder(BookingExtractor.class).chatModel(model).build();
        this.trace = trace;
        this.clock = clock;
        this.dataDir = dataDir;
    }

    @Override public Scenario scenario() { return Scenario.ROOMS; }
    @Override public List<Object> tools() { return List.of(tools); }
    @Override public List<InputGuardrail> inputGuardrails() { return List.of(new ScopeInputGuardrail(trace)); }
    @Override public List<OutputGuardrail> outputGuardrails() { return List.of(new PhantomBookingGuardrail(bookings, trace)); }
    @Override public String structuredActionLabel() { return "Check booking"; }
    @Override public String defaultUserId() { return "S2026001"; }
    @Override public List<Path> dataFiles() { return List.of(bookings.file(), dataDir.resolve("rooms.json")); }

    @Override
    public StructuredResult runStructured(String userId, String userText) throws RuleViolationException {
        LocalDate today = LocalDate.now(clock);
        BookingRequest req = extractor.extract(userText, today + " (" + today.getDayOfWeek() + ")");
        trace.log("structured", Map.of("pojo", String.valueOf(req)));
        if (req == null || req.roomCode() == null || req.roomCode().isBlank()) {
            throw new RuleViolationException("FORMAT", "Please name a room code, e.g. C7.03.");
        }
        try {
            Booking preview = service.validate(userId, req.roomCode(), DateTimes.parseDate(req.date()),
                    DateTimes.parseTime(req.startTime()), req.durationMinutes(), req.groupSize(), bookings.findAll(), "(preview)");
            return new StructuredResult("Extracted: " + JsonStore.gson().toJson(req) + "\nValid: " + preview.roomCode() + " "
                    + preview.date() + " " + preview.start() + "-" + preview.end() + " (not booked yet).",
                    () -> tools.bookRoom(userId, req.roomCode(), req.date(), req.startTime(), req.durationMinutes(), req.groupSize()));
        } catch (DataAccessException e) {
            throw new RuleViolationException("DATA", e.getMessage());
        }
    }
}
