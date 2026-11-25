package my.edu.taylors.ap.scenario.textbooks;

import my.edu.taylors.ap.config.GroupProfile;

import java.util.Map;

/**
 * @param depreciationPercent  T1 Y (% per year)
 * @param supersededPercent    T1 Z (% extra for superseded editions)
 * @param floorSen             T1 F (minimum suggested price)
 * @param platformFeePercent   T3 P
 */
public record TextbookRules(int depreciationPercent, int supersededPercent, int floorSen, int platformFeePercent) {

    public static final Map<String, Integer> CONDITION_PERCENT = Map.of("A", 100, "B", 85, "C", 65, "D", 40);
    public static final int MAX_MARKUP_PERCENT = 20;

    public static TextbookRules from(GroupProfile g) {
        return new TextbookRules(g.param("Y"), g.param("Z"), g.param("F"), g.param("P"));
    }
}
