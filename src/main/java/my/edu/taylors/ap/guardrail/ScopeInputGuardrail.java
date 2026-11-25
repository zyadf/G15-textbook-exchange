package my.edu.taylors.ap.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * WORKED EXAMPLE of an INPUT guardrail. It runs BEFORE the message reaches the model.
 * <ul>
 *   <li>rejects empty or oversized messages (cost and abuse control)</li>
 *   <li>rejects common prompt-injection phrasings</li>
 * </ul>
 * A failure stops the call: AiServices throws {@code InputGuardrailException}, which the
 * controller catches and shows to the user. Extend the patterns from your red-team testing.
 * <p>
 * Subtle point (ask yourself why in the viva): input guardrails run AFTER retrieval, so
 * {@code request.userMessage()} already contains the RAG excerpts. We therefore check the
 * ORIGINAL text, which AiServices exposes through the template variable {@code {{message}}}
 * declared on {@code ScenarioAssistant.chat}.
 */
public class ScopeInputGuardrail implements InputGuardrail {

    public static final int MAX_CHARS = 1_000;

    static final Pattern INJECTION = Pattern.compile(
            "ignore (all |any |the )?(previous|prior|above|your) (instructions|rules)"
                    + "|disregard (your|the|all) (rules|instructions)"
                    + "|(reveal|print|show|repeat) (me )?(your|the) (system )?(prompt|instructions)"
                    + "|you are now|developer mode|jailbreak",
            Pattern.CASE_INSENSITIVE);

    private final TraceLogger trace;

    public ScopeInputGuardrail(TraceLogger trace) {
        this.trace = trace;
    }

    @Override
    public InputGuardrailResult validate(InputGuardrailRequest request) {
        Object original = request.requestParams() == null ? null : request.requestParams().variables().get("message");
        if (original != null) {
            return check(original.toString());
        }
        return validate(request.userMessage());
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        return check(userMessage.hasSingleText() ? userMessage.singleText() : "");
    }

    InputGuardrailResult check(String text) {
        if (text == null || text.isBlank()) {
            return block("empty", "Please type a message.");
        }
        if (text.length() > MAX_CHARS) {
            return block("too long", "Messages are limited to " + MAX_CHARS + " characters.");
        }
        if (INJECTION.matcher(text).find()) {
            return block("prompt injection", "I can only help with this service's own requests.");
        }
        trace.log("guardrail.input", Map.of("result", "pass"));
        return success();
    }

    private InputGuardrailResult block(String reason, String userFacing) {
        trace.log("guardrail.input", Map.of("result", "blocked", "reason", reason));
        return failure(userFacing);
    }
}
