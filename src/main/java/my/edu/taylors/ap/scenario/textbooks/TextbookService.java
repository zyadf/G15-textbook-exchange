package my.edu.taylors.ap.scenario.textbooks;

import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.textbooks.TextbookExceptions.InvalidConditionException;
import my.edu.taylors.ap.scenario.textbooks.TextbookExceptions.InvalidPriceException;
import my.edu.taylors.ap.scenario.textbooks.TextbookExceptions.MissingDisclosureException;
import my.edu.taylors.ap.scenario.textbooks.TextbookExceptions.OverpricedListingException;
import my.edu.taylors.ap.scenario.textbooks.TextbookExceptions.UnknownBookException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Locale;

/** Deterministic rules T1-T4. BigDecimal is used for the compound depreciation. */
public class TextbookService {

    private final List<Book> books;
    private final TextbookRules rules;
    private final Clock clock;

    public TextbookService(List<Book> books, TextbookRules rules, Clock clock) {
        this.books = List.copyOf(books);
        this.rules = rules;
        this.clock = clock;
    }

    public List<Book> books() {
        return books;
    }

    public Book book(String isbn) throws UnknownBookException {
        String digits = isbn == null ? "" : isbn.replaceAll("\\D", "");
        return books.stream().filter(b -> b.isbn().equals(digits)).findFirst()
                .orElseThrow(() -> new UnknownBookException(isbn));
    }

    public List<Book> search(String keyword) {
        String k = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        return books.stream().filter(b -> b.title().toLowerCase(Locale.ROOT).contains(k) || b.isbn().contains(k)).toList();
    }

    public static String normaliseCondition(String c) throws InvalidConditionException {
        String g = c == null ? "" : c.trim().toUpperCase(Locale.ROOT);
        if (!TextbookRules.CONDITION_PERCENT.containsKey(g)) {
            throw new InvalidConditionException(c);
        }
        return g;
    }

    /** T1 suggested price in sen. */
    public long suggestedSen(Book b, String condition) throws InvalidConditionException {
        String grade = normaliseCondition(condition);
        int age = Math.max(0, Year.now(clock).getValue() - b.year());
        BigDecimal keep = BigDecimal.valueOf(100 - rules.depreciationPercent()).movePointLeft(2);   // e.g. 0.88
        BigDecimal raw = BigDecimal.valueOf(b.listPriceSen())
                .multiply(keep.pow(age, MathContext.DECIMAL64))
                .multiply(BigDecimal.valueOf(TextbookRules.CONDITION_PERCENT.get(grade)).movePointLeft(2));
        if (b.superseded()) {
            raw = raw.multiply(BigDecimal.valueOf(100 - rules.supersededPercent()).movePointLeft(2));
        }
        long roundedDown50 = raw.divide(BigDecimal.valueOf(50), 0, RoundingMode.FLOOR).longValueExact() * 50;
        return Math.max(rules.floorSen(), roundedDown50);
    }

    /** T2: maximum asking price = suggested + 20%, rounded down to the sen. */
    public long maxAskingSen(long suggested) {
        return suggested * (100 + TextbookRules.MAX_MARKUP_PERCENT) / 100;
    }

    /** T2 + T4 validation of a new listing. */
    public void validateListing(Book b, String condition, long askingSen, String defects) throws RuleViolationException {
        String grade = normaliseCondition(condition);
        long max = maxAskingSen(suggestedSen(b, grade));
        if (askingSen > max) {
            throw new OverpricedListingException(Money.rm(askingSen), Money.rm(max));
        }
        if ("D".equals(grade) && (defects == null || defects.isBlank())) {
            throw new MissingDisclosureException();
        }
    }

    /** T3: asking + platform fee (half-up to the sen). */
    public long buyerTotalSen(long askingSen) {
        return askingSen + Money.percentOf(askingSen, rules.platformFeePercent());
    }

    public static long parseRinggit(String raw) throws InvalidPriceException {
        try {
            String clean = raw.trim().replaceFirst("(?i)^rm\\s*", "");
            return new BigDecimal(clean).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (NumberFormatException | ArithmeticException | NullPointerException e) {
            throw new InvalidPriceException(raw);
        }
    }
}
