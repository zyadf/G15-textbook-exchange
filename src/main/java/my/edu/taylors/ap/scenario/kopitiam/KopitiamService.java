package my.edu.taylors.ap.scenario.kopitiam;

import my.edu.taylors.ap.config.HouseRules;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamExceptions.InvalidQuantityException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamExceptions.ItemUnavailableException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamExceptions.OrderFormatException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamExceptions.RushHourLimitException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamExceptions.UnknownMenuItemException;
import my.edu.taylors.ap.scenario.kopitiam.OrderRequest.OrderLine;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deterministic business logic for the kopitiam. NO LLM in here: prices, limits and promotions
 * are computed in Java so they are exact and testable. The model only decides WHEN to call it.
 */
public class KopitiamService {

    private final Map<String, MenuItem> menuByCode;
    private final KopitiamRules rules;
    private final Clock clock;

    public KopitiamService(List<MenuItem> menu, KopitiamRules rules, Clock clock) {
        this.menuByCode = menu.stream().collect(Collectors.toMap(
                m -> m.code().toUpperCase(Locale.ROOT), Function.identity(), (a, b) -> a, LinkedHashMap::new));
        this.rules = rules;
        this.clock = clock;
    }

    public List<MenuItem> menu() {
        return List.copyOf(menuByCode.values());
    }

    public MenuItem find(String code) throws UnknownMenuItemException {
        MenuItem item = menuByCode.get(code == null ? "" : code.trim().toUpperCase(Locale.ROOT));
        if (item == null) {
            throw new UnknownMenuItemException(code);
        }
        return item;
    }

    public List<MenuItem> search(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return menuByCode.values().stream()
                .filter(m -> k.isEmpty() || m.name().toLowerCase(Locale.ROOT).contains(k)
                        || m.code().equalsIgnoreCase(k) || m.category().equalsIgnoreCase(k))
                .toList();
    }

    /** Parses "D01x2, F03x1" into order lines. */
    public static List<OrderLine> parseItems(String items) throws OrderFormatException {
        if (items == null || items.isBlank()) {
            throw new OrderFormatException(String.valueOf(items));
        }
        List<OrderLine> lines = new ArrayList<>();
        for (String raw : items.split(",")) {
            String part = raw.trim();
            String[] codeQty = part.split("(?i)\\s*x\\s*");
            if (codeQty.length != 2) {
                throw new OrderFormatException(part);
            }
            try {
                lines.add(new OrderLine(codeQty[0].trim(), Integer.parseInt(codeQty[1].trim())));
            } catch (NumberFormatException e) {
                throw new OrderFormatException(part);
            }
        }
        return lines;
    }

    /** Applies house rules K1-K4 and returns the priced order. */
    public Quote quote(List<OrderLine> requested, boolean dineIn) throws RuleViolationException {
        // 1. Validate and merge duplicate codes (e.g. "D01x1, D01x2" -> D01 x3)
        Map<String, Integer> qty = new LinkedHashMap<>();
        for (OrderLine l : requested) {
            MenuItem item = find(l.itemCode());
            if (!item.available()) {
                throw new ItemUnavailableException(item);
            }
            if (l.quantity() < 1 || l.quantity() > 20) {
                throw new InvalidQuantityException(item.code(), l.quantity());
            }
            qty.merge(item.code(), l.quantity(), Integer::sum);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int minute = now.getHour() * 60 + now.getMinute();
        boolean weekday = now.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue();

        // 2. K2 rush-hour cap
        int tehTarik = qty.getOrDefault(KopitiamRules.TEH_TARIK, 0);
        if (weekday && minute < rules.rushHourEndMinute() && tehTarik > rules.rushHourTehTarikCap()) {
            throw new RushHourLimitException(tehTarik, rules.rushHourTehTarikCap(),
                    HouseRules.clock(rules.rushHourEndMinute()));
        }

        // 3. Subtotal
        List<Quote.Line> lines = new ArrayList<>();
        long subtotal = 0;
        int drinkUnits = 0;
        for (Map.Entry<String, Integer> e : qty.entrySet()) {
            MenuItem item = menuByCode.get(e.getKey());
            long lineSen = (long) item.priceSen() * e.getValue();
            subtotal += lineSen;
            if (item.isDrink()) {
                drinkUnits += e.getValue();
            }
            lines.add(new Quote.Line(item.code(), item.name(), e.getValue(), item.priceSen(), lineSen));
        }

        // 4. K1 afternoon treat (free item does not change the subtotal)
        boolean treat = minute >= KopitiamRules.TREAT_START_MINUTE && minute < KopitiamRules.TREAT_END_MINUTE
                && subtotal >= rules.freeToastThresholdSen();
        if (treat) {
            MenuItem toast = find(KopitiamRules.KAYA_TOAST);
            lines.add(new Quote.Line(toast.code(), toast.name() + " (K1 treat)", 1, 0, 0));
        }

        // 5. K3 charges
        long service = dineIn ? Money.percentOf(subtotal, rules.serviceChargePercent()) : 0;
        long containers = dineIn ? 0 : (long) KopitiamRules.CONTAINER_FEE_SEN * drinkUnits;

        // 6. K4 rounding
        long unrounded = subtotal + service + containers;
        return new Quote(List.copyOf(lines), subtotal, treat, service, containers, unrounded,
                Money.roundToNearest5Sen(unrounded), dineIn);
    }
}
