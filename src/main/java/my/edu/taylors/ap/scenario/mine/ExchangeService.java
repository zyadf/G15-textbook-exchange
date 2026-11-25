// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions.InvalidConditionException;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions.InvalidPriceException;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions.MissingDefectsException;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions.OverpricedListingException;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions.UnknownBookException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Year;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic business logic for house rules T1-T4. There is NO language model in this class:
 * every number is computed in Java so it is exact, repeatable and unit-testable. The model only
 * decides WHEN to call the tools that use this service.
 * <p>
 * Money is held in whole sen ({@code long}). T1 needs a fractional power ((1 - Y%)^age), so the
 * intermediate value is computed with {@link BigDecimal}, which is EXACT: 0.91^5 is
 * 0.6240321451 with no binary rounding error, so a value such as RM94.9995 is never pushed over
 * RM95.00 by accident.
 */
public class ExchangeService {

    static final String QUILLMARK = "generated";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final Map<String, Book> booksByIsbn = new LinkedHashMap<>();
    private final ExchangeRules rules;
    private final Clock clock;

    public ExchangeService(List<Book> books, ExchangeRules rules, Clock clock) {
        for (Book b : books) {
            booksByIsbn.putIfAbsent(b.isbn(), b);
        }
        this.rules = rules;
        this.clock = clock;
    }

    public ExchangeRules rules() {
        return rules;
    }

    public List<Book> books() {
        return List.copyOf(booksByIsbn.values());
    }

    // ------------------------------------------------------------------ look-ups

    /** Finds a book by ISBN; spaces and hyphens are ignored ("978-0-00..." works). */
    public Book book(String isbn) throws UnknownBookException {
        String digits = isbn == null ? "" : isbn.replaceAll("[\\s-]", "");
        Book b = booksByIsbn.get(digits);
        if (b == null) {
            throw new UnknownBookException(isbn);
        }
        return b;
    }

    /** Case-insensitive search by title words or ISBN; a blank keyword lists every book. */
    public List<Book> search(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return booksByIsbn.values().stream()
                .filter(b -> k.isEmpty() || b.title().toLowerCase(Locale.ROOT).contains(k) || b.isbn().contains(k))
                .toList();
    }

    /** Accepts "b", " B " or "grade B" and returns "B"; anything else breaks T1. */
    public static String normaliseGrade(String raw) throws InvalidConditionException {
        String g = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replaceFirst("^GRADE\\s*", "");
        if (!ExchangeRules.CONDITION_PERCENT.containsKey(g)) {
            throw new InvalidConditionException(raw);
        }
        return g;
    }

    // ------------------------------------------------------------------ T1 and T2

    /** T1 age = current year - publication year, never negative. Uses the injected clock. */
    public int age(Book b) {
        return Math.max(0, Year.now(clock).getValue() - b.year());
    }

    /**
     * T1: list x (1 - Y%)^age x condition factor, a further -Z% for a superseded edition,
     * rounded DOWN to RM0.50, never below RM{F}. Also returns the T2 maximum asking price.
     */
    public PriceSuggestion suggest(Book b, String condition) throws InvalidConditionException {
        String grade = normaliseGrade(condition);
        int age = age(b);

        BigDecimal keepPerYear = BigDecimal.valueOf(100 - rules.depreciationPercent()).divide(HUNDRED); // 0.91
        BigDecimal value = BigDecimal.valueOf(b.listPriceSen())
                .multiply(keepPerYear.pow(age))                                                       // exact power
                .multiply(BigDecimal.valueOf(ExchangeRules.CONDITION_PERCENT.get(grade)).divide(HUNDRED));
        if (b.isSuperseded()) {
            value = value.multiply(BigDecimal.valueOf(100 - rules.supersededPercent()).divide(HUNDRED));
        }

        long step = ExchangeRules.ROUNDING_STEP_SEN;
        long roundedDown = value.divide(BigDecimal.valueOf(step), 0, RoundingMode.FLOOR).longValueExact() * step;
        boolean floorApplied = roundedDown < rules.floorSen();
        long suggested = floorApplied ? rules.floorSen() : roundedDown;

        return new PriceSuggestion(b, grade, age, value.doubleValue(), suggested, floorApplied, maxAskingSen(suggested));
    }

    /** T2: at most 20% above the suggested price, rounded DOWN to the sen (never allow more than 20%). */
    public long maxAskingSen(long suggestedSen) {
        return Math.floorDiv(suggestedSen * (100 + ExchangeRules.MAX_MARKUP_PERCENT), 100);
    }

    // ------------------------------------------------------------------ T2 + T4 validation

    /**
     * Checks a new listing against T1 (grade), T2 (price limit) and T4 (defects for grade D).
     * Throws the first rule that is broken; returns the T1/T2 numbers when everything is valid.
     */
    public PriceSuggestion validateListing(Book b, String condition, long askingSen, String defects)
            throws InvalidConditionException, InvalidPriceException, OverpricedListingException,
            MissingDefectsException {
        if (askingSen <= 0) {
            throw new InvalidPriceException(Money.rm(askingSen));
        }
        PriceSuggestion s = suggest(b, condition);
        if (askingSen > s.maxAskingSen()) {
            throw new OverpricedListingException(Money.rm(askingSen), Money.rm(s.suggestedSen()), Money.rm(s.maxAskingSen()));
        }
        if ("D".equals(s.grade()) && (defects == null || defects.isBlank())) {
            throw new MissingDefectsException();
        }
        return s;
    }

    // ------------------------------------------------------------------ T3

    /** T3 platform fee: P% of the asking price, rounded half-up to the sen. */
    public long platformFeeSen(long askingSen) {
        return Money.percentOf(askingSen, rules.platformFeePercent());
    }

    /** T3: what the buyer pays = asking price + platform fee. */
    public long buyerTotalSen(long askingSen) {
        return askingSen + platformFeeSen(askingSen);
    }

    // ------------------------------------------------------------------ helpers

    /** "45", "45.5", "RM 45.50" -> 4550 sen. Rejects text, negative numbers and fractions of a sen. */
    public static long parseRinggit(String raw) throws InvalidPriceException {
        if (raw == null) {
            throw new InvalidPriceException("null");
        }
        String clean = raw.trim().replaceFirst("(?i)^rm\\s*", "").replace(",", "");
        try {
            BigDecimal sen = new BigDecimal(clean).movePointRight(2);
            if (sen.signum() <= 0 || sen.stripTrailingZeros().scale() > 0) {
                throw new InvalidPriceException(raw);
            }
            return sen.longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new InvalidPriceException(raw);
        }
    }

    /** Next free ID "TX-nnnn" = highest existing number + 1 (safe even if a listing is removed later). */
    public static String nextListingId(Collection<Listing> existing) {
        int max = 0;
        for (Listing l : existing) {
            String id = l.listingId() == null ? "" : l.listingId();
            if (id.matches("TX-\\d+")) {
                max = Math.max(max, Integer.parseInt(id.substring(3)));
            }
        }
        return String.format("TX-%04d", max + 1);
    }
}
