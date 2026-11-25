package my.edu.taylors.ap.scenario.textbooks;

import my.edu.taylors.ap.scenario.common.RuleViolationException;

/** Custom checked exceptions for the textbook exchange scenario. */
public final class TextbookExceptions {

    private TextbookExceptions() { }

    public static class UnknownBookException extends RuleViolationException {
        public UnknownBookException(String isbn) {
            super("BOOK", "No book with ISBN '" + isbn + "' in the catalogue.");
        }
    }

    public static class InvalidConditionException extends RuleViolationException {
        public InvalidConditionException(String c) {
            super("T1", "Condition must be A, B, C or D (got '" + c + "').");
        }
    }

    public static class OverpricedListingException extends RuleViolationException {
        public OverpricedListingException(String asking, String max) {
            super("T2", "Asking price " + asking + " is more than 20% above the suggested price; the maximum is " + max + ".");
        }
    }

    public static class MissingDisclosureException extends RuleViolationException {
        public MissingDisclosureException() {
            super("T4", "Condition D listings must describe the defects.");
        }
    }

    public static class UnknownListingException extends RuleViolationException {
        public UnknownListingException(String id) {
            super("LISTING", "No listing with ID '" + id + "'.");
        }
    }

    public static class InvalidPriceException extends RuleViolationException {
        public InvalidPriceException(String raw) {
            super("FORMAT", "Cannot read price '" + raw + "'. Use ringgit like 45.00.");
        }
    }
}
