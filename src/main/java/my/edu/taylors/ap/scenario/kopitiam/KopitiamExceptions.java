package my.edu.taylors.ap.scenario.kopitiam;

import my.edu.taylors.ap.scenario.common.RuleViolationException;

/** Custom checked exceptions for the kopitiam scenario (one class per failure the user can fix). */
public final class KopitiamExceptions {

    private KopitiamExceptions() { }

    public static class UnknownMenuItemException extends RuleViolationException {
        public UnknownMenuItemException(String code) {
            super("MENU", "There is no menu item with code '" + code + "'. Use searchMenu to find valid codes.");
        }
    }

    public static class ItemUnavailableException extends RuleViolationException {
        public ItemUnavailableException(MenuItem item) {
            super("MENU", item.name() + " (" + item.code() + ") is sold out today.");
        }
    }

    public static class InvalidQuantityException extends RuleViolationException {
        public InvalidQuantityException(String code, int qty) {
            super("QTY", "Quantity " + qty + " for " + code + " is invalid; it must be between 1 and 20.");
        }
    }

    public static class RushHourLimitException extends RuleViolationException {
        public RushHourLimitException(int requested, int cap, String until) {
            super("K2", "Rush-hour limit: at most " + cap + " Teh Tarik per order on weekdays before " + until
                    + " (requested " + requested + ").");
        }
    }

    public static class OrderFormatException extends RuleViolationException {
        public OrderFormatException(String fragment) {
            super("FORMAT", "Cannot read '" + fragment + "'. Use CODExQTY separated by commas, e.g. D01x2,F03x1.");
        }
    }
}
