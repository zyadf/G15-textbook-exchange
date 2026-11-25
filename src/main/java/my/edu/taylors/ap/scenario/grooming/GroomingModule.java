package my.edu.taylors.ap.scenario.grooming;

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
import java.util.stream.Collectors;

/** Scenario 4 wiring. */
public class GroomingModule implements ScenarioModule {

    interface AppointmentExtractor {
        @UserMessage("""
                Today is {{today}}. Known breeds: {{breeds}}.
                Extract the grooming appointment request from: {{text}}""")
        AppointmentRequest extract(@V("text") String text, @V("today") String today, @V("breeds") String breeds);
    }

    private final GroomingService service;
    private final GroomingTools tools;
    private final JsonStore<Appointment> appointments;
    private final AppointmentExtractor extractor;
    private final String breedList;
    private final TraceLogger trace;
    private final Clock clock;
    private final JsonStore<Pet> pets;
    private final Path dataDir;

    public GroomingModule(GroupProfile profile, Path dataDir, Path runtimeDir, ChatModel model,
                          TraceLogger trace, Clock clock) throws DataAccessException {
        List<Breed> breeds = JsonStore.of(dataDir.resolve("breeds.json"), Breed.class).findAll();
        this.service = new GroomingService(
                JsonStore.of(dataDir.resolve("services.json"), GroomService.class).findAll(),
                breeds,
                JsonStore.of(dataDir.resolve("groomers.json"), Groomer.class).findAll(),
                GroomingRules.from(profile));
        this.appointments = JsonStore.seeded(dataDir.resolve("appointments.json"), runtimeDir.resolve("appointments.json"), Appointment.class);
        this.pets = JsonStore.of(dataDir.resolve("pets.json"), Pet.class);
        this.tools = new GroomingTools(service, pets, appointments);
        this.extractor = AiServices.builder(AppointmentExtractor.class).chatModel(model).build();
        this.breedList = breeds.stream().map(Breed::breed).collect(Collectors.joining(", "));
        this.trace = trace;
        this.clock = clock;
        this.dataDir = dataDir;
    }

    @Override public Scenario scenario() { return Scenario.GROOMING; }
    @Override public List<Object> tools() { return List.of(tools); }
    @Override public List<InputGuardrail> inputGuardrails() { return List.of(new ScopeInputGuardrail(trace)); }
    @Override public List<OutputGuardrail> outputGuardrails() { return List.of(new NoVetAdviceGuardrail(trace)); }
    @Override public String structuredActionLabel() { return "Check appointment"; }
    @Override public String defaultUserId() { return "OWN-01"; }
    @Override public List<Path> dataFiles() { return List.of(appointments.file(), dataDir.resolve("pets.json"), dataDir.resolve("services.json")); }

    @Override
    public StructuredResult runStructured(String userId, String userText) throws RuleViolationException {
        LocalDate today = LocalDate.now(clock);
        AppointmentRequest req = extractor.extract(userText, today + " (" + today.getDayOfWeek() + ")", breedList);
        trace.log("structured", Map.of("pojo", String.valueOf(req)));
        if (req == null || req.breed() == null || req.serviceCode() == null) {
            throw new RuleViolationException("FORMAT", "Please mention the breed and the service you want.");
        }
        Estimate est = service.estimate(req.breed(), req.serviceCode(), req.firstVisit());
        try {
            Groomer g = service.assignGroomer(est.size(), DateTimes.parseDate(req.date()),
                    DateTimes.parseTime(req.startTime()), est.minutes(), appointments.findAll());
            String summary = "Extracted: " + JsonStore.gson().toJson(req) + "\n" + est.describe() + "\nGroomer available: "
                    + g.name() + " (not booked yet).";
            // Commit only if this owner has a registered pet of that breed
            return pets.findWhere(p -> p.ownerId().equalsIgnoreCase(userId) && p.breed().equalsIgnoreCase(req.breed()))
                    .stream().findFirst()
                    .map(p -> new StructuredResult(summary + "\nPet: " + p.petId() + " " + p.name(),
                            () -> tools.book(userId, p.petId(), req.serviceCode(), req.date(), req.startTime())))
                    .orElseGet(() -> StructuredResult.infoOnly(summary + "\nNo registered pet of this breed for " + userId + "; register at the counter."));
        } catch (DataAccessException e) {
            throw new RuleViolationException("DATA", e.getMessage());
        }
    }
}
