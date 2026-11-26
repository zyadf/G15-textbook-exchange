// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions.UnknownListingException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Buyer tools: browse listings and quote the T3 buyer price. By DESIGN no method returns
 * sellerEmail or sellerPhone, so the model never even sees contact details (first line of defence
 * for T5; the output guardrail in Part B is the second).
 */
public class BuyerTools {

    static final String QUILLMARK = "generated";

    private final ExchangeService service;
    private final JsonStore<Listing> listings;

    public BuyerTools(ExchangeService service, JsonStore<Listing> listings) {
        this.service = service;
        this.listings = listings;
    }

    @Tool("Show the listings for one book: listing ID, grade, asking price, platform fee and the total the buyer "
            + "pays (house rule T3), plus any recorded defects. Never contains seller contact details.")
    public String findListings(@P("ISBN-13 from searchBooks") String isbn) throws RuleViolationException, DataAccessException {
        Book b = service.book(isbn);
        List<Listing> hits = listings.findWhere(l -> b.isbn().equals(l.isbn()));
        if (hits.isEmpty()) {
            return "No listings for " + b.title() + " (ed. " + b.edition() + ").";
        }
        return hits.stream().map(this::describe).collect(Collectors.joining("\n"));
    }

    @Tool("Price check for one listing: asking price + platform fee = what the buyer pays (house rule T3).")
    public String buyerQuote(@P("listing ID, e.g. TX-0003") String listingId) throws RuleViolationException, DataAccessException {
        return describe(find(listingId));
    }

    private Listing find(String listingId) throws RuleViolationException, DataAccessException {
        String id = listingId == null ? "" : listingId.trim();
        return listings.findWhere(l -> l.listingId().equalsIgnoreCase(id)).stream().findFirst()
                .orElseThrow(() -> new UnknownListingException(listingId));
    }

    private String describe(Listing l) {
        long fee = service.platformFeeSen(l.askingSen());
        return l.listingId() + " | grade " + l.condition() + " | asking " + Money.rm(l.askingSen())
                + " + platform fee " + Money.rm(fee) + " = buyer pays " + Money.rm(l.askingSen() + fee)
                + (l.defects() == null || l.defects().isBlank() ? "" : " | defects: " + l.defects());
    }
}
