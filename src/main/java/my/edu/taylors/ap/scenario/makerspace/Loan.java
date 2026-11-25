package my.edu.taylors.ap.scenario.makerspace;

/**
 * Row of data/loans.json (seed data) and runtime/loans.json (live data).
 * Times are ISO-8601 local date-times, e.g. "2026-11-02T14:00". {@code returnedAt} is null while active.
 */
public record Loan(String loanId, String memberId, String equipmentCode, String start, String due, String returnedAt) {

    public boolean active() {
        return returnedAt == null || returnedAt.isBlank();
    }

    public Loan returned(String when) {
        return new Loan(loanId, memberId, equipmentCode, start, due, when);
    }
}
