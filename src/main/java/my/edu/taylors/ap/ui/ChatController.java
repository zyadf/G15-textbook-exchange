package my.edu.taylors.ap.ui;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import my.edu.taylors.ap.AgentRuntime;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.StructuredResult;
import my.edu.taylors.ap.ui.ConversationModel.Role;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * CONTROLLER: handles UI events and talks to the agent.
 * <p>
 * LLM calls take seconds, so they NEVER run on the JavaFX Application Thread (the UI would
 * freeze). Each call runs in a {@link Task} on a background thread; results are applied back on
 * the FX thread by the Task's event handlers.
 */
public class ChatController {

    private final ConversationModel model;
    private final AgentRuntime runtime;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "agent-worker");
        t.setDaemon(true);
        return t;
    });
    private Predicate<String> confirmer = summary -> false;   // replaced by the view
    private Runnable afterEachCall = () -> { };              // e.g. refresh the Data tab
    private Task<?> current;

    public ChatController(ConversationModel model, AgentRuntime runtime) {
        this.model = model;
        this.runtime = runtime;
        // trace events arrive on the worker thread -> hop to the FX thread before touching the model
        runtime.trace().addObserver(line -> Platform.runLater(() -> model.addTrace(line)));
    }

    /** The view supplies a dialog that asks the user "commit this?" and returns the answer. */
    public void setConfirmer(Predicate<String> confirmer) {
        this.confirmer = confirmer;
    }

    public void setAfterEachCall(Runnable r) {
        this.afterEachCall = r;
    }

    /** Send button / Enter key. */
    public void onSend(String text) {
        if (text == null || text.isBlank() || model.busyProperty().get()) {
            return;
        }
        String user = model.userIdProperty().get().trim();
        model.add(Role.USER, text);
        run(() -> runtime.assistant().chat(user, text), reply -> model.add(Role.AGENT, reply));
    }

    /**
     * Structured-output button: extract a POJO from the text, validate it with the house rules,
     * show it, and commit it only if the USER confirms.
     */
    public void onStructured(String text) {
        if (text == null || text.isBlank() || model.busyProperty().get()) {
            return;
        }
        String user = model.userIdProperty().get().trim();
        model.add(Role.USER, "[" + runtime.module().structuredActionLabel() + "] " + text);
        run(() -> runtime.module().runStructured(user, text), result -> {
            model.add(Role.SYSTEM, result.summary());
            if (result.committable()) {
                boolean yes = confirmer.test(result.summary());
                runtime.trace().log("structured.confirm", Map.of("user", user, "confirmed", yes));
                if (yes) {
                    run(result.commit(), done -> model.add(Role.SYSTEM, done));
                } else {
                    model.add(Role.SYSTEM, "Not committed.");
                }
            }
        });
    }

    /** Forget the current user's conversation (deletes runtime/memory/<user>.json). */
    public void onClearMemory() {
        String user = model.userIdProperty().get().trim();
        runtime.memoryStore().deleteMessages(user);
        runtime.trace().log("memory", Map.of("event", "cleared", "user", user));
        model.add(Role.SYSTEM, "Memory cleared for " + user + ".");
    }

    /** Cancel button: stop waiting for a slow model. */
    public void onCancel() {
        if (current != null && current.isRunning()) {
            current.cancel(true);
        }
    }

    private <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        current = task;
        model.busyProperty().set(true);
        task.setOnSucceeded(e -> {
            model.busyProperty().set(false);
            onSuccess.accept(task.getValue());
            afterEachCall.run();
        });
        task.setOnFailed(e -> {
            model.busyProperty().set(false);
            Throwable error = task.getException();
            runtime.trace().log("error", Map.of("where", "ui", "error", String.valueOf(error)));
            model.add(Role.ERROR, explain(error));
            afterEachCall.run();
        });
        task.setOnCancelled(e -> {
            model.busyProperty().set(false);
            runtime.trace().log("cancelled", Map.of("by", "user"));
            model.add(Role.SYSTEM, "Cancelled. (The model may still finish in the background; its answer is ignored.)");
        });
        worker.submit(task);
    }

    /** Turns exceptions into messages a user can act on. */
    static String explain(Throwable error) {
        if (error instanceof InputGuardrailException) {
            return "Blocked by input guardrail: " + error.getMessage();
        }
        if (error instanceof OutputGuardrailException) {
            return "The agent's answer failed a safety check and was withheld. Please rephrase your question.";
        }
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof RuleViolationException rv) {
                return "House rule " + rv.ruleId() + ": " + rv.getMessage();
            }
            if (t instanceof ConnectException) {
                return "Cannot reach the model server. Is Ollama / LM Studio running? (" + t.getMessage() + ")";
            }
            if (t instanceof HttpTimeoutException) {
                return "The model took too long to answer. Try a smaller model or raise llm.timeoutSeconds.";
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return "Something went wrong: " + error.getClass().getSimpleName() + " - " + error.getMessage();
    }

    public void shutdown() {
        worker.shutdownNow();
    }
}
