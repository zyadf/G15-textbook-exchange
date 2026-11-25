package my.edu.taylors.ap.scenario.makerspace;

import my.edu.taylors.ap.scenario.common.RuleViolationException;

/** Custom checked exceptions for the makerspace scenario. */
public final class MakerspaceExceptions {

    private MakerspaceExceptions() { }

    public static class UnknownMemberException extends RuleViolationException {
        public UnknownMemberException(String id) {
            super("MEMBER", "'" + id + "' is not a registered member ID (format MKR-nnnn).");
        }
    }

    public static class UnknownEquipmentException extends RuleViolationException {
        public UnknownEquipmentException(String code) {
            super("EQUIPMENT", "No equipment with code '" + code + "'. Use findEquipment first.");
        }
    }

    public static class SafetyQuizRequiredException extends RuleViolationException {
        public SafetyQuizRequiredException(Equipment e) {
            super("M1", e.name() + " requires a passed safety quiz before it can be loaned.");
        }
    }

    public static class LoanLimitExceededException extends RuleViolationException {
        public LoanLimitExceededException(int limit) {
            super("M3", "Members may hold at most " + limit + " active loans. Return an item first.");
        }
    }

    public static class OutOfStockException extends RuleViolationException {
        public OutOfStockException(Equipment e) {
            super("STOCK", "All units of " + e.name() + " are currently on loan.");
        }
    }

    public static class NotYourLoanException extends RuleViolationException {
        public NotYourLoanException(String loanId) {
            super("M5", "Loan " + loanId + " does not belong to you.");
        }
    }
}
