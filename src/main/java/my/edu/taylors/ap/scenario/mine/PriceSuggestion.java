// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import my.edu.taylors.ap.scenario.common.Money;

/**
 * Result of rule T1 with every intermediate step kept, so the tool can show its working and
 * the group can compare it with the hand calculations in the design report (A1).
 *
 * @param book          the book that was priced
 * @param grade         normalised condition grade A-D
 * @param age           current year - publication year (never negative)
 * @param unroundedSen  list x (1-Y%)^age x condition [x (1-Z%)], before rounding, as exact sen
 * @param suggestedSen  T1 result: rounded DOWN to RM0.50, never below the floor
 * @param floorApplied  true if the floor price {F} was used
 * @param maxAskingSen  T2 limit: suggested + 20%
 */
public record PriceSuggestion(Book book, String grade, int age, double unroundedSen, long suggestedSen,
                              boolean floorApplied, long maxAskingSen) {

    static final String QUILLMARK = "generated";

    /** Short text for the model. unroundedSen is a double ONLY for display; it is never used for money. */
    public String describe() {
        return String.format("%s (ed. %d, %d) grade %s: age %d year(s)%s, before rounding RM%.4f, "
                        + "suggested %s%s, maximum asking price %s.",
                book.title(), book.edition(), book.year(), grade, age,
                book.isSuperseded() ? ", superseded edition" : "",
                unroundedSen / 100.0, Money.rm(suggestedSen), floorApplied ? " (floor price)" : "",
                Money.rm(maxAskingSen));
    }
}
