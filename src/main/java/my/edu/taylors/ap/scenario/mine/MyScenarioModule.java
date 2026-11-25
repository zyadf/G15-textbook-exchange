package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.model.chat.ChatModel;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.guardrail.ScopeInputGuardrail;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.scenario.common.StructuredResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Your scenario's wiring. The application calls these methods; everything scenario-specific
 * is created here. Work through the TODOs in the order given in the assignment brief.
 */
public class MyScenarioModule implements ScenarioModule {

    private final GroupProfile profile;
    private final Path dataDir;
    private final Path runtimeDir;
    private final ChatModel model;
    private final TraceLogger trace;
    private final Clock clock;
    private final ExampleTools exampleTools;

    public MyScenarioModule(GroupProfile profile, Path dataDir, Path runtimeDir, ChatModel model,
                            TraceLogger trace, Clock clock) throws DataAccessException {
        this.profile = profile;
        this.dataDir = dataDir;
        this.runtimeDir = runtimeDir;
        this.model = model;
        this.trace = trace;
        this.clock = clock;
        this.exampleTools = new ExampleTools(profile, clock, dataDir);
        // TODO [A6] load your data files with JsonStore (use JsonStore.seeded(...) for files the tools CHANGE,
        //           so the seed data in scenarios/ is never modified) and create your service + tools.
        // TODO [B3] create your structured-output extractor:
        //           AiServices.builder(YourExtractor.class).chatModel(model).build();
    }

    @Override
    public Scenario scenario() {
        return profile.scenario();
    }

    @Override
    public List<Object> tools() {
        List<Object> tools = new ArrayList<>();
        tools.add(exampleTools);
        // TODO [A6] add your own tools object(s) here, e.g. tools.add(myTools);
        return tools;
    }

    @Override
    public List<InputGuardrail> inputGuardrails() {
        // TODO [B4] add further input guardrails from your threat model (Part A, A5)
        return List.of(new ScopeInputGuardrail(trace));
    }

    @Override
    public List<OutputGuardrail> outputGuardrails() {
        // TODO [B4] replace with your guardrail for the fifth house rule
        return List.of(new MyOutputGuardrail(trace));
    }

    @Override
    public String structuredActionLabel() {
        return "Extract request";      // TODO [B3] e.g. "Extract order", "Check booking"
    }

    @Override
    public String defaultUserId() {
        return "customer1";            // TODO [A6] an ID that exists in your sample data, if your scenario has users
    }

    @Override
    public List<Path> dataFiles() {
        // TODO [B6] list your runtime files first, then the read-only data files
        return List.of(dataDir);
    }

    @Override
    public StructuredResult runStructured(String userId, String userText) throws RuleViolationException {
        // TODO [B3] 1. call your extractor AI Service -> POJO
        //           2. trace.log("structured", Map.of("pojo", String.valueOf(pojo)))
        //           3. validate the POJO with your service (house rules) - throw RuleViolationException subclasses
        //           4. return new StructuredResult(summary, () -> yourTools.commitSomething(...))
        throw new RuleViolationException("TODO", "Structured output is not implemented yet (Part B, task B3).");
    }
}
