package my.edu.taylors.ap.scenario.makerspace;

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
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.scenario.common.StructuredResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Scenario 2 wiring. Live loans are kept in runtime/loans.json, seeded from data/loans.json on first run. */
public class MakerspaceModule implements ScenarioModule {

    interface LoanExtractor {
        @UserMessage("""
                Extract the equipment loan request. Choose the code from this catalogue: {{catalogue}}
                Message: {{text}}""")
        LoanRequest extract(@V("text") String text, @V("catalogue") String catalogue);
    }

    private final MakerspaceService service;
    private final MakerspaceTools tools;
    private final JsonStore<Loan> loans;
    private final LoanExtractor extractor;
    private final List<Member> members;
    private final TraceLogger trace;
    private final Path dataDir;

    public MakerspaceModule(GroupProfile profile, Path dataDir, Path runtimeDir, ChatModel model,
                            TraceLogger trace, Clock clock) throws DataAccessException {
        List<Equipment> equipment = JsonStore.of(dataDir.resolve("equipment.json"), Equipment.class).findAll();
        this.members = JsonStore.of(dataDir.resolve("members.json"), Member.class).findAll();
        this.service = new MakerspaceService(equipment, members, MakerspaceRules.from(profile), clock);
        this.loans = JsonStore.seeded(dataDir.resolve("loans.json"), runtimeDir.resolve("loans.json"), Loan.class);
        this.tools = new MakerspaceTools(service, loans, clock);
        this.extractor = AiServices.builder(LoanExtractor.class).chatModel(model).build();
        this.trace = trace;
        this.dataDir = dataDir;
    }

    @Override public Scenario scenario() { return Scenario.MAKERSPACE; }
    @Override public List<Object> tools() { return List.of(tools); }
    @Override public List<InputGuardrail> inputGuardrails() { return List.of(new ScopeInputGuardrail(trace)); }
    @Override public List<OutputGuardrail> outputGuardrails() { return List.of(new MemberPrivacyGuardrail(members, trace)); }
    @Override public String structuredActionLabel() { return "Check loan request"; }
    @Override public String defaultUserId() { return "MKR-0113"; }
    @Override public List<Path> dataFiles() { return List.of(loans.file(), dataDir.resolve("equipment.json"), dataDir.resolve("members.json")); }

    @Override
    public StructuredResult runStructured(String userId, String userText) throws RuleViolationException {
        String catalogue = service.find("").stream().map(e -> e.code() + "=" + e.name()).collect(Collectors.joining(", "));
        LoanRequest req = extractor.extract(userText, catalogue);
        trace.log("structured", Map.of("pojo", String.valueOf(req)));
        if (req == null || req.equipmentCode() == null || req.equipmentCode().isBlank()) {
            throw new RuleViolationException("FORMAT", "Could not identify which equipment you want.");
        }
        try {
            Loan preview = service.prepareLoan(userId, req.equipmentCode(), loans.findAll(), "(preview)");
            return new StructuredResult("Extracted: " + JsonStore.gson().toJson(req) + "\nEligible. If borrowed now it is due "
                    + preview.due().replace('T', ' ') + ".", () -> tools.borrow(userId, req.equipmentCode()));
        } catch (DataAccessException e) {
            throw new RuleViolationException("DATA", e.getMessage());
        }
    }
}
