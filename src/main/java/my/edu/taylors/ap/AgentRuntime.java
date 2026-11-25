package my.edu.taylors.ap;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolProvider;
import my.edu.taylors.ap.config.AppConfig;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.GroupSeeder;
import my.edu.taylors.ap.llm.AssistantFactory;
import my.edu.taylors.ap.llm.ModelFactory;
import my.edu.taylors.ap.llm.ScenarioAssistant;
import my.edu.taylors.ap.mcp.McpSupport;
import my.edu.taylors.ap.memory.FileChatMemoryStore;
import my.edu.taylors.ap.rag.KnowledgeBase;
import my.edu.taylors.ap.scenario.ScenarioModules;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.trace.TraceChatModelListener;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Composition root: builds every object once, in the right order, and hands the finished
 * assistant to the UI. This is "dependency injection by hand" - what Spring would do for you.
 */
public final class AgentRuntime implements AutoCloseable {

    private final AppConfig config;
    private final GroupProfile profile;
    private final TraceLogger trace;
    private final ScenarioModule module;
    private final ScenarioAssistant assistant;
    private final FileChatMemoryStore memoryStore;
    private final McpSupport mcp;

    private AgentRuntime(AppConfig config, GroupProfile profile, TraceLogger trace, ScenarioModule module,
                         ScenarioAssistant assistant, FileChatMemoryStore memoryStore, McpSupport mcp) {
        this.config = config;
        this.profile = profile;
        this.trace = trace;
        this.module = module;
        this.assistant = assistant;
        this.memoryStore = memoryStore;
        this.mcp = mcp;
    }

    public static AgentRuntime start(AppConfig config, Clock clock) throws Exception {
        GroupProfile profile = GroupSeeder.profileFor(config.groupNumber());
        Path runtime = config.runtimeDir();
        // The trace ALWAYS uses the real clock (it is evidence), even when rules use a fixed test clock.
        TraceLogger trace = new TraceLogger(runtime.resolve("traces"), profile.watermarkId());

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("event", "start");
        session.put("scenario", profile.scenario().name());
        session.put("provider", config.provider().name());
        session.put("model", config.modelName());
        session.put("java", System.getProperty("java.version"));
        session.put("os", System.getProperty("os.name"));
        session.put("user", System.getProperty("user.name"));
        session.put("ruleClock", config.fixedClock().isBlank() ? "system" : "fixed " + config.fixedClock());
        trace.log("session", session);

        ChatModel model = ModelFactory.create(config, List.of(new TraceChatModelListener(trace)));
        Path scenarioDir = config.scenariosDir().resolve(profile.scenario().folder());
        ScenarioModule module = ScenarioModules.create(profile, scenarioDir, runtime, model, trace, clock);

        // TODO [B2] enable RAG once KnowledgeBase is implemented:
        KnowledgeBase kb = KnowledgeBase.openOrBuild(scenarioDir.resolve("docs"), runtime.resolve("embeddings.json"));
        ContentRetriever retriever = kb.retriever();
        trace.log("rag", Map.of("rebuilt", kb.wasRebuilt()));

        // ContentRetriever retriever = null;
        // trace.log("rag", Map.of("enabled", retriever != null));

        FileChatMemoryStore memory = new FileChatMemoryStore(runtime.resolve("memory"));

        McpSupport mcp = null;
        ToolProvider extra = null;
        if (config.mcpEnabled()) {
            mcp = McpSupport.filesystem(runtime.resolve("exports"));
            extra = mcp.toolProvider();
            trace.log("mcp", Map.of("enabled", true, "tools", McpSupport.ALLOWED_TOOLS));
        }
        ScenarioAssistant assistant = AssistantFactory.build(model, module, retriever, memory, extra, trace, profile, clock);
        return new AgentRuntime(config, profile, trace, module, assistant, memory, mcp);
    }

    public AppConfig config() { return config; }
    public GroupProfile profile() { return profile; }
    public TraceLogger trace() { return trace; }
    public ScenarioModule module() { return module; }
    public ScenarioAssistant assistant() { return assistant; }
    public FileChatMemoryStore memoryStore() { return memoryStore; }

    @Override
    public void close() throws Exception {
        trace.log("session", Map.of("event", "stop"));
        if (mcp != null) {
            mcp.close();
        }
    }
}
