package my.edu.taylors.ap.scenario.kopitiam;

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

/** Scenario 1 wiring: data files, service, tools, guardrails and the structured-output extractor. */
public class KopitiamModule implements ScenarioModule {

    /** STRUCTURED OUTPUT AI Service: returns a POJO instead of a String. */
    interface OrderExtractor {
        @UserMessage("""
                Extract the kopitiam order from the customer's message.
                Map every item to one of these menu codes: {{menu}}
                Customer message: {{text}}""")
        OrderRequest extract(@V("text") String text, @V("menu") String menu);
    }

    private final KopitiamService service;
    private final KopitiamTools tools;
    private final OrderExtractor extractor;
    private final TraceLogger trace;
    private final Path dataDir;
    private final Path runtimeDir;

    public KopitiamModule(GroupProfile profile, Path dataDir, Path runtimeDir, ChatModel model,
                          TraceLogger trace, Clock clock) throws DataAccessException {
        List<MenuItem> menu = JsonStore.of(dataDir.resolve("menu.json"), MenuItem.class).findAll();
        this.service = new KopitiamService(menu, KopitiamRules.from(profile), clock);
        this.tools = new KopitiamTools(service,
                JsonStore.of(runtimeDir.resolve("orders.json"), KopitiamTools.PlacedOrder.class), clock);
        this.extractor = AiServices.builder(OrderExtractor.class).chatModel(model).build();
        this.trace = trace;
        this.dataDir = dataDir;
        this.runtimeDir = runtimeDir;
    }

    @Override public Scenario scenario() { return Scenario.KOPITIAM; }

    @Override public List<Object> tools() { return List.of(tools); }

    @Override public List<InputGuardrail> inputGuardrails() { return List.of(new ScopeInputGuardrail(trace)); }

    @Override public List<OutputGuardrail> outputGuardrails() { return List.of(new AllergenHonestyGuardrail(trace)); }

    @Override public String structuredActionLabel() { return "Extract order"; }
    @Override public List<Path> dataFiles() { return List.of(runtimeDir.resolve("orders.json"), dataDir.resolve("menu.json")); }

    @Override
    public StructuredResult runStructured(String userId, String userText) throws RuleViolationException {
        String codes = service.menu().stream().map(m -> m.code() + "=" + m.name()).collect(Collectors.joining(", "));
        OrderRequest req = extractor.extract(userText, codes);
        trace.log("structured", Map.of("pojo", String.valueOf(req)));
        if (req == null || req.lines() == null || req.lines().isEmpty()) {
            throw new RuleViolationException("FORMAT", "No order items could be extracted from the message.");
        }
        Quote q = service.quote(req.lines(), req.dineIn());
        String items = req.lines().stream().map(l -> l.itemCode() + "x" + l.quantity()).collect(Collectors.joining(","));
        String name = req.customerName() == null || req.customerName().isBlank() ? userId : req.customerName();
        return new StructuredResult("Extracted: " + JsonStore.gson().toJson(req) + "\n\n" + q.describe(),
                () -> tools.placeOrder(items, req.dineIn(), name));
    }
}
