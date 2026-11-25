package my.edu.taylors.ap.scenario.common;

/**
 * Base CHECKED exception for every house-rule violation (e.g. "K2 rush-hour cap exceeded").
 * <p>
 * Each scenario defines its own subclasses. When a {@code @Tool} method throws one, the
 * AssistantFactory's error handler sends {@link #getMessage()} back to the model so it can
 * explain the problem to the user, and the violation is recorded in the trace.
 */
public class RuleViolationException extends Exception {

    private final String ruleId;

    public RuleViolationException(String ruleId, String message) {
        super(ruleId + ": " + message);
        this.ruleId = ruleId;
    }

    /** e.g. "K2" */
    public String ruleId() {
        return ruleId;
    }
}
