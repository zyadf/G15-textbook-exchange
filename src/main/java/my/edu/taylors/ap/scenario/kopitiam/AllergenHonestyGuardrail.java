package my.edu.taylors.ap.scenario.kopitiam;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import my.edu.taylors.ap.trace.TraceLogger;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * OUTPUT guardrail for house rule K5. Runs on the model's final answer before the user sees it.
 * If the answer claims an item is allergen-free/safe, or discusses allergies without referring
 * the customer to staff, the model is asked to rewrite it (reprompt).
 */
public class AllergenHonestyGuardrail implements OutputGuardrail {

    static final Pattern UNSAFE_CLAIM = Pattern.compile(
            "allergen[- ]?free|no allergens|free (of|from) (all )?allergens|(is|are|it's|totally|completely) safe"
                    + "|safe for (people|anyone|someone|you|those)|nut[- ]?free|peanut[- ]?free|gluten[- ]?free|dairy[- ]?free",
            Pattern.CASE_INSENSITIVE);

    private final TraceLogger trace;

    public AllergenHonestyGuardrail(TraceLogger trace) {
        this.trace = trace;
    }

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String text = responseFromLLM.text() == null ? "" : responseFromLLM.text();
        String lower = text.toLowerCase(Locale.ROOT);
        if (UNSAFE_CLAIM.matcher(text).find()) {
            trace.log("guardrail.output", Map.of("rule", "K5", "result", "reprompt", "reason", "unsafe allergen claim"));
            return reprompt("K5: unsafe allergen claim",
                    "Rewrite your answer. Do NOT say any item is allergen-free, safe, nut-free or gluten-free. "
                            + "List only the allergens returned by the getAllergens tool and tell the customer to confirm with our staff.");
        }
        if (lower.contains("allerg") && !lower.contains("staff")) {
            trace.log("guardrail.output", Map.of("rule", "K5", "result", "reprompt", "reason", "no staff referral"));
            return reprompt("K5: allergy answer without staff referral",
                    "Rewrite your answer and advise the customer to confirm allergens with our staff.");
        }
        return success();
    }
}
