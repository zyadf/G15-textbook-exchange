// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Seller tools: create a listing (T1, T2, T4 checked first) and list the current user's own
 * listings. The seller id comes from {@code @ToolMemoryId}, i.e. the user id typed in the UI, so the
 * model cannot create or read listings on behalf of somebody else.
 */
public class ListingTools {

    static final String QUILLMARK = "generated";

    private final ExchangeService service;
    private final JsonStore<Listing> listings;

    public ListingTools(ExchangeService service, JsonStore<Listing> listings) {
        this.service = service;
        this.listings = listings;
    }

    @Tool("Create a listing for the CURRENT user after they have agreed to the grade and price. Checks the "
            + "house rules first (T2 price limit, T4 defects for grade D). Returns the new listing ID (TX-nnnn).")
    public String createListing(@ToolMemoryId String sellerId,
                                @P("ISBN-13 from searchBooks") String isbn,
                                @P("condition grade: A, B, C or D") String condition,
                                @P("asking price in ringgit, e.g. 45.00") String askingPriceRm,
                                @P("description of defects; required for grade D, otherwise empty") String defects)
            throws RuleViolationException, DataAccessException {
        Book b = service.book(isbn);
        long asking = ExchangeService.parseRinggit(askingPriceRm);
        PriceSuggestion s = service.validateListing(b, condition, asking, defects);
        synchronized (listings) {                       // two quick calls must not get the same ID
            List<Listing> all = listings.findAll();
            Listing l = new Listing(ExchangeService.nextListingId(all), b.isbn(), sellerId, "", "",
                    s.grade(), asking, defects == null ? "" : defects.trim());
            listings.append(l);
            return "Listing " + l.listingId() + " created: " + b.title() + ", grade " + s.grade()
                    + ", asking " + Money.rm(asking) + " (buyer pays " + Money.rm(service.buyerTotalSen(asking)) + ").";
        }
    }

    @Tool("List the listings that belong to the CURRENT user only.")
    public String myListings(@ToolMemoryId String sellerId) throws DataAccessException {
        List<Listing> mine = listings.findWhere(l -> l.sellerId() != null && l.sellerId().equalsIgnoreCase(sellerId));
        if (mine.isEmpty()) {
            return "You have no listings.";
        }
        return mine.stream()
                .map(l -> l.listingId() + " | ISBN " + l.isbn() + " | grade " + l.condition() + " | asking " + Money.rm(l.askingSen()))
                .collect(Collectors.joining("\n"));
    }
}
