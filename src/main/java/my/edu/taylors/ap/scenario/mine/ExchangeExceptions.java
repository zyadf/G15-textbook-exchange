// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import my.edu.taylors.ap.scenario.common.RuleViolationException;

/**
 * One CHECKED exception per thing the user can get wrong. Each extends RuleViolationException,
 * so when a tool throws it the AssistantFactory sends "RULE VIOLATION ..." back to the model
 * (which then explains it) and records "rule.violation" in the trace.
 */
public final class ExchangeExceptions {

    static final String QUILLMARK = "generated";

    private ExchangeExceptions() { }

    /** The ISBN is not in books.json (the model may invent ISBNs). */
    public static class UnknownBookException extends RuleViolationException {
        public UnknownBookException(String isbn) {
            super("BOOK", "No book with ISBN '" + isbn + "' is in the catalogue. Use searchBooks to find the ISBN.");
        }
    }

    /** T1: the grade must be A, B, C or D. */
    public static class InvalidConditionException extends RuleViolationException {
        public InvalidConditionException(String grade) {
            super("T1", "Condition must be A, B, C or D (got '" + grade + "').");
        }
    }

    /** T2: asking price more than 20% above the suggested price. */
    public static class OverpricedListingException extends RuleViolationException {
        public OverpricedListingException(String asking, String suggested, String max) {
            super("T2", "Asking price " + asking + " is more than 20% above the suggested price " + suggested
                    + ". The maximum allowed is " + max + ".");
        }
    }

    /** T4: a grade D listing needs a non-empty defects description. */
    public static class MissingDefectsException extends RuleViolationException {
        public MissingDefectsException() {
            super("T4", "Condition D listings must include a description of the defects "
                    + "(for example missing pages or a detached cover).");
        }
    }

    /** Price text that is not a positive ringgit amount, e.g. "fifty" or "-5". */
    public static class InvalidPriceException extends RuleViolationException {
        public InvalidPriceException(String raw) {
            super("FORMAT", "Cannot use price '" + raw + "'. Give a positive amount in ringgit, e.g. 45.00.");
        }
    }

    /** Listing ID that does not exist. */
    public static class UnknownListingException extends RuleViolationException {
        public UnknownListingException(String id) {
            super("LISTING", "No listing with ID '" + id + "'. Use findListings to see the listing IDs.");
        }
    }
}
