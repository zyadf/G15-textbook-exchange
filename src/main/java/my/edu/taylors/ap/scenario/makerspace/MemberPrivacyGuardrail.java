package my.edu.taylors.ap.scenario.makerspace;

import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OUTPUT guardrail for M5: the answer must not mention any OTHER member's ID or name.
 * The current member is read from the chat memory id in the guardrail request.
 */
public class MemberPrivacyGuardrail implements OutputGuardrail {

    private final List<Member> members;
    private final TraceLogger trace;

    public MemberPrivacyGuardrail(List<Member> members, TraceLogger trace) {
        this.members = members;
        this.trace = trace;
    }

    @Override
    public OutputGuardrailResult validate(OutputGuardrailRequest request) {
        String text = request.responseFromLLM().aiMessage().text();
        Object me = request.requestParams() != null && request.requestParams().chatMemory() != null
                ? request.requestParams().chatMemory().id() : "";
        return check(text == null ? "" : text, String.valueOf(me));
    }

    OutputGuardrailResult check(String text, String currentMemberId) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (Member m : members) {
            if (m.memberId().equalsIgnoreCase(currentMemberId)) {
                continue;
            }
            if (lower.contains(m.memberId().toLowerCase(Locale.ROOT)) || lower.contains(m.name().toLowerCase(Locale.ROOT))) {
                trace.log("guardrail.output", Map.of("rule", "M5", "result", "reprompt"));
                return reprompt("M5: another member's details in answer",
                        "Rewrite the answer without naming or identifying any other member.");
            }
        }
        return success();
    }
}
