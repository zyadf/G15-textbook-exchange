package my.edu.taylors.ap.scenario.kopitiam;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tools the kopitiam agent may call. Each method is small: it validates input, delegates to
 * {@link KopitiamService} and returns plain text the model can relay.
 * Tool descriptions matter: the model decides which tool to call from them.
 */
public class KopitiamTools {

    /** Persisted order (runtime/orders.json). */
    public record PlacedOrder(String orderId, String customerName, String items, boolean dineIn,
                              long totalSen, String placedAt) { }

    private final KopitiamService service;
    private final JsonStore<PlacedOrder> orders;
    private final Clock clock;

    public KopitiamTools(KopitiamService service, JsonStore<PlacedOrder> orders, Clock clock) {
        this.service = service;
        this.orders = orders;
        this.clock = clock;
    }

    @Tool("Search the menu by keyword, item code or category (DRINK, FOOD, SNACK). Returns codes, names and prices.")
    public String searchMenu(@P("keyword, code or category; empty string lists everything") String keyword) {
        List<MenuItem> hits = service.search(keyword);
        if (hits.isEmpty()) {
            return "No menu items match '" + keyword + "'.";
        }
        return hits.stream()
                .map(m -> m.code() + " " + m.name() + " " + Money.rm(m.priceSen()) + (m.available() ? "" : " [SOLD OUT]"))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Get the allergens recorded for one menu item. Always use this before answering any allergy question.")
    public String getAllergens(@P("menu item code, e.g. F01") String itemCode) throws RuleViolationException {
        MenuItem item = service.find(itemCode);
        String list = item.allergens().isEmpty() ? "none recorded in our data" : String.join(", ", item.allergens());
        return item.name() + ": allergens " + list + ". Data may be incomplete - customers must confirm with staff.";
    }

    @Tool("Price an order without placing it. Applies all house rules (promotions, limits, charges, rounding).")
    public String quoteOrder(@P("items as CODExQTY separated by commas, e.g. D01x2,F03x1") String items,
                             @P("true for dine-in, false for takeaway") boolean dineIn) throws RuleViolationException {
        return service.quote(KopitiamService.parseItems(items), dineIn).describe();
    }

    @Tool("Place an order after the customer has confirmed the quote. Returns the order ID.")
    public String placeOrder(@P("items as CODExQTY separated by commas, e.g. D01x2,F03x1") String items,
                             @P("true for dine-in, false for takeaway") boolean dineIn,
                             @P("customer's first name") String customerName)
            throws RuleViolationException, DataAccessException {
        Quote q = service.quote(KopitiamService.parseItems(items), dineIn);
        List<PlacedOrder> all = orders.findAll();
        String id = String.format("KO-%04d", all.size() + 1);
        orders.append(new PlacedOrder(id, customerName, items, dineIn, q.totalSen(), Instant.now(clock).toString()));
        return "Order " + id + " placed. Total " + Money.rm(q.totalSen()) + ".";
    }
}
