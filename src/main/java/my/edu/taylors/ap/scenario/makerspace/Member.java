package my.edu.taylors.ap.scenario.makerspace;

/** Row of data/members.json (synthetic data). */
public record Member(String memberId, String name, boolean safetyQuizPassed) {

    /** "MKR-0113" -> 113 */
    public int numericId() {
        return Integer.parseInt(memberId.replaceAll("\\D", ""));
    }
}
