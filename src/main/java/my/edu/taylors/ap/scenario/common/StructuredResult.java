package my.edu.taylors.ap.scenario.common;

import java.util.concurrent.Callable;

/**
 * What a structured-output action returns to the UI: a human-readable summary of the POJO the
 * model produced (already validated by the house rules) and, optionally, the action that would
 * commit it. The UI asks the USER to confirm before {@code commit} runs - the model never
 * commits a structured request by itself (human-in-the-loop).
 *
 * @param summary text shown to the user
 * @param commit  action that places/books/lists the request, or null if nothing can be committed
 */
public record StructuredResult(String summary, Callable<String> commit) {

    public static StructuredResult infoOnly(String summary) {
        return new StructuredResult(summary, null);
    }

    public boolean committable() {
        return commit != null;
    }
}
