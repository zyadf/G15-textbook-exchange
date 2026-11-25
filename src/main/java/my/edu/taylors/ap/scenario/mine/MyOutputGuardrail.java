package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.util.Map;

/**
 * TODO [B4] OUTPUT guardrail for your scenario's fifth house rule (K5 / M5 / R5 / P5 / T5).
 * <p>
 * It receives the model's final answer BEFORE the user sees it and returns one of:
 * {@code success()}, {@code reprompt(reason, instructionToModel)} (model rewrites its answer),
 * {@code retry(reason)} or {@code fatal(reason)}.
 * Rename the class to describe what it protects, write unit tests for it (no model needed),
 * and record every decision in the trace as shown below.
 */
public class MyOutputGuardrail implements OutputGuardrail {

    private final TraceLogger trace;

    public MyOutputGuardrail(TraceLogger trace) {
        this.trace = trace;
    }

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text() == null ? "" : responseFromLLM.text();
        // TODO [B4] detect a violation of your fifth house rule in 'text', e.g.
        // if (violates(text)) {
        //     trace.log("guardrail.output", Map.of("rule", "K5", "result", "reprompt"));
        //     return reprompt("K5 violated", "Rewrite your answer so that ...");
        // }
        trace.log("guardrail.output", Map.of("result", "pass", "chars", text.length()));
        return success();
    }
}
