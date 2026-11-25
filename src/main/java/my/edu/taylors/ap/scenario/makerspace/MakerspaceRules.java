package my.edu.taylors.ap.scenario.makerspace;

import my.edu.taylors.ap.config.GroupProfile;

/**
 * @param baseLoanHours       M2 base loan period B (hours)
 * @param maxActiveLoans      M3 limit C
 * @param lateFeeSenPerHour   M4 fee F (sen per started hour)
 */
public record MakerspaceRules(int baseLoanHours, int maxActiveLoans, int lateFeeSenPerHour) {

    public static final int ODD_ID_BONUS_HOURS = 12;
    public static final int PRINTER_HOURS = 8;
    public static final int LATE_FEE_CAP_SEN = 3000;

    public static MakerspaceRules from(GroupProfile g) {
        return new MakerspaceRules(g.param("B"), g.param("C"), g.param("F"));
    }
}
