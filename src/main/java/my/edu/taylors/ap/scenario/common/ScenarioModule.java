package my.edu.taylors.ap.scenario.common;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import my.edu.taylors.ap.config.Scenario;

import java.nio.file.Path;
import java.util.List;

/**
 * Everything scenario-specific that the generic agent needs. Each group implements ONE module
 * for its scenario; the rest of the application (UI, memory, RAG, trace) is shared.
 */
public interface ScenarioModule {

    Scenario scenario();

    /** Objects whose {@code @Tool} methods the agent may call. */
    List<Object> tools();

    List<InputGuardrail> inputGuardrails();

    List<OutputGuardrail> outputGuardrails();

    /** Label of the UI button that runs structured extraction, e.g. "Extract order". */
    String structuredActionLabel();

    /** User id pre-filled in the UI (e.g. a member ID that exists in the sample data). */
    default String defaultUserId() {
        return "customer1";
    }

    /** Data files shown in the UI's "Data" tab (live runtime files first). */
    List<Path> dataFiles();

    /**
     * Runs the scenario's structured-output AI Service on free text, validates the resulting POJO
     * against the house rules, and returns a summary plus the action that would commit it.
     *
     * @param userId   the current user (same id the chat memory uses)
     * @param userText free text typed by the user
     * @throws RuleViolationException if the extracted request breaks a house rule
     */
    StructuredResult runStructured(String userId, String userText) throws RuleViolationException;
}
