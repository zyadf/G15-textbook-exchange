package my.edu.taylors.ap.llm;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.HouseRules;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.trace.TraceLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Assembles the {@link ScenarioAssistant} from its parts. This is the one place where the
 * LangChain4j features meet: model + system message + memory + tools + RAG + guardrails + trace.
 */
public final class AssistantFactory {

    public static final int MEMORY_WINDOW = 20;
    public static final int MAX_TOOL_ROUND_TRIPS = 8;

    private AssistantFactory() { }

    public static ScenarioAssistant build(ChatModel model,
                                          ScenarioModule module,
                                          ContentRetriever retriever,
                                          ChatMemoryStore memoryStore,
                                          ToolProvider extraTools,      // may be null (MCP bonus)
                                          TraceLogger trace,
                                          GroupProfile profile,
                                          Clock clock) {
        String systemMessage = systemMessage(profile, clock);
        AiServices<ScenarioAssistant> builder = AiServices.builder(ScenarioAssistant.class)
                .chatModel(model)
                .systemMessageProvider(memoryId -> systemMessage)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(MEMORY_WINDOW)
                        .chatMemoryStore(memoryStore)
                        .build())
                .tools(module.tools())
                .inputGuardrails(module.inputGuardrails())
                .outputGuardrails(module.outputGuardrails())
                .maxToolCallingRoundTrips(MAX_TOOL_ROUND_TRIPS)
                .beforeToolExecution(before -> trace.log("tool.call", Map.of(
                        "tool", before.request().name(),
                        "args", String.valueOf(before.request().arguments()))))
                .afterToolExecution(done -> trace.log("tool.result", Map.of(
                        "tool", done.request().name(),
                        "failed", done.hasFailed(),
                        "result", clip(done.result()))))
                .toolExecutionErrorHandler((error, ctx) -> {
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("tool", ctx.toolExecutionRequest().name());
                    d.put("error", error.getClass().getSimpleName());
                    d.put("message", String.valueOf(error.getMessage()));
                    trace.log(error instanceof RuleViolationException ? "rule.violation" : "error", d);
                    // House-rule violations are safe and useful for the model to explain.
                    // Anything else is internal: do not leak stack details to the model.
                    return ToolErrorHandlerResult.text(error instanceof RuleViolationException
                            ? "RULE VIOLATION " + error.getMessage()
                            : "The tool failed because of an internal error. Apologise and suggest trying again later.");
                });
        if (retriever != null) {
            builder.contentRetriever(retriever);      // RAG (Part B)
        }
        if (extraTools != null) {
            builder.toolProvider(extraTools);
        }
        return builder.build();
    }

    static String systemMessage(GroupProfile profile, Clock clock) {
        String template;
        try (InputStream in = AssistantFactory.class.getResourceAsStream("/my/edu/taylors/ap/system-prompt.txt")) {
            if (in == null) {
                throw new IllegalStateException("system-prompt.txt resource is missing");
            }
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        StringBuilder rules = new StringBuilder();
        for (String r : HouseRules.forProfile(profile)) {
            rules.append("- ").append(r).append('\n');
        }
        return template
                .replace("{{scenario}}", profile.scenario().title())
                .replace("{{today}}", LocalDate.now(clock)
                        .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH)))
                .replace("{{group}}", profile.groupCode())
                .replace("{{rules}}", rules.toString().trim());
    }

    private static String clip(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 400 ? s.substring(0, 397) + "..." : s;
    }
}
