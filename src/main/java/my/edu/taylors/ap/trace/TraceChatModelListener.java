package my.edu.taylors.ap.trace;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Records every round-trip to the LLM in the trace. Attached to the ChatModel in
 * {@code ModelFactory}, so it sees requests from every AI Service.
 */
public class TraceChatModelListener implements ChatModelListener {

    private final TraceLogger trace;

    public TraceChatModelListener(TraceLogger trace) {
        this.trace = trace;
    }

    @Override
    public void onRequest(ChatModelRequestContext ctx) {
        List<ChatMessage> messages = ctx.chatRequest().messages();
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("messages", messages.size());
        d.put("tools", ctx.chatRequest().toolSpecifications() == null ? 0 : ctx.chatRequest().toolSpecifications().size());
        if (!messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            d.put("lastType", last.type().toString());
            d.put("last", clip(String.valueOf(last)));
        }
        trace.log("model.request", d);
    }

    @Override
    public void onResponse(ChatModelResponseContext ctx) {
        AiMessage ai = ctx.chatResponse().aiMessage();
        Map<String, Object> d = new LinkedHashMap<>();
        if (ai.text() != null) {
            d.put("text", clip(ai.text()));
        }
        if (ai.hasToolExecutionRequests()) {
            d.put("toolRequests", ai.toolExecutionRequests().stream()
                    .map(TraceChatModelListener::describe).toList());
        }
        d.put("finish", String.valueOf(ctx.chatResponse().finishReason()));
        trace.log("model.response", d);
    }

    @Override
    public void onError(ChatModelErrorContext ctx) {
        trace.log("error", Map.of("where", "model", "error", String.valueOf(ctx.error())));
    }

    static String describe(ToolExecutionRequest r) {
        return r.name() + " " + r.arguments();
    }

    static String clip(String s) {
        return s.length() > 600 ? s.substring(0, 597) + "..." : s;
    }
}
