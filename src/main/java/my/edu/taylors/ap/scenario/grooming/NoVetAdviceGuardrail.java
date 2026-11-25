package my.edu.taylors.ap.scenario.grooming;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.util.Map;
import java.util.regex.Pattern;

/** OUTPUT guardrail for P5: no diagnosis or medication advice; health questions go to a vet. */
public class NoVetAdviceGuardrail implements OutputGuardrail {

    static final Pattern MEDICAL = Pattern.compile(
            "\\b(\\d+\\s?mg|dosage|dose|antibiotic|steroid|ointment|medicat(e|ion)|prescri|diagnos|it (is|sounds like|looks like) (an? )?(infection|allergy|dermatitis|mange))",
            Pattern.CASE_INSENSITIVE);

    private final TraceLogger trace;

    public NoVetAdviceGuardrail(TraceLogger trace) {
        this.trace = trace;
    }

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text() == null ? "" : responseFromLLM.text();
        if (MEDICAL.matcher(text).find()) {
            trace.log("guardrail.output", Map.of("rule", "P5", "result", "reprompt"));
            return reprompt("P5: veterinary advice",
                    "Rewrite without any diagnosis, medicine or dosage. Say our groomers cannot give medical advice "
                            + "and recommend seeing a veterinarian.");
        }
        return success();
    }
}
