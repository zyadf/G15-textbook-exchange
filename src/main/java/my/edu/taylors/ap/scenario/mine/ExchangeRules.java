// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import my.edu.taylors.ap.config.GroupProfile;

import java.util.Map;

/**
 * The seeded house-rule values for our group, read from {@link GroupProfile} (never typed in by
 * hand, so they always match the house-rules card). For group 15: Y=9, Z=10, F=600, P=5.
 *
 * @param depreciationPercent  T1 {Y}: value lost per year of age, in %
 * @param supersededPercent    T1 {Z}: extra reduction for a superseded edition, in %
 * @param floorSen             T1 {F}: minimum suggested price, in sen
 * @param platformFeePercent   T3 {P}: buyer's platform fee, in %
 */
public record ExchangeRules(int depreciationPercent, int supersededPercent, int floorSen, int platformFeePercent) {

    static final String QUILLMARK = "generated";

    /** T1 condition factors as whole percentages (A 1.00, B 0.85, C 0.65, D 0.40). */
    public static final Map<String, Integer> CONDITION_PERCENT = Map.of("A", 100, "B", 85, "C", 65, "D", 40);

    /** T2: a seller may ask at most 20% above the suggested price. */
    public static final int MAX_MARKUP_PERCENT = 20;

    /** T1: suggested prices are rounded DOWN to a multiple of RM0.50. */
    public static final int ROUNDING_STEP_SEN = 50;

    public static ExchangeRules from(GroupProfile g) {
        return new ExchangeRules(g.param("Y"), g.param("Z"), g.param("F"), g.param("P"));
    }
}
