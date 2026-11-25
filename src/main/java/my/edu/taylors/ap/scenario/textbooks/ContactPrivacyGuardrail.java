package my.edu.taylors.ap.scenario.textbooks;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.util.Map;
import java.util.regex.Pattern;

/** OUTPUT guardrail for T5: no e-mail addresses or Malaysian phone numbers in answers. */
public class ContactPrivacyGuardrail implements OutputGuardrail {

    static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    static final Pattern PHONE = Pattern.compile("(\\+?6?0)1\\d[-\\s]?\\d{3,4}[-\\s]?\\d{4}");

    private final TraceLogger trace;

    public ContactPrivacyGuardrail(TraceLogger trace) {
        this.trace = trace;
    }

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text() == null ? "" : responseFromLLM.text();
        if (EMAIL.matcher(text).find() || PHONE.matcher(text).find()) {
            trace.log("guardrail.output", Map.of("rule", "T5", "result", "reprompt"));
            return reprompt("T5: contact details in answer",
                    "Rewrite without any e-mail address or phone number. Meet-ups are arranged through the platform.");
        }
        return success();
    }
}
