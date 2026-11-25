package my.edu.taylors.ap.scenario.kopitiam;

import my.edu.taylors.ap.config.GroupProfile;

/**
 * The seeded values for one group's kopitiam house rules.
 *
 * @param freeToastThresholdSen K1 threshold T (sen)
 * @param rushHourTehTarikCap   K2 cap N
 * @param rushHourEndMinute     K2 end of rush hour H (minutes after midnight)
 * @param serviceChargePercent  K3 service charge S (%)
 */
public record KopitiamRules(int freeToastThresholdSen, int rushHourTehTarikCap, int rushHourEndMinute, int serviceChargePercent) {

    public static final String TEH_TARIK = "D01";
    public static final String KAYA_TOAST = "K01";
    public static final int CONTAINER_FEE_SEN = 20;
    public static final int TREAT_START_MINUTE = 15 * 60;      // 15:00
    public static final int TREAT_END_MINUTE = 18 * 60;        // up to 17:59

    public static KopitiamRules from(GroupProfile g) {
        return new KopitiamRules(g.param("T"), g.param("N"), g.param("H"), g.param("S"));
    }
}
