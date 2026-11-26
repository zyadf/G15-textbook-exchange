// AI-ASSISTED: Claude (Anthropic), 2026-11-26
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions.UnknownListingException;

/**
 * Contact tool for house rule T5: a buyer says they want a listing, and the PLATFORM arranges the
 * meet-up. The reply never contains the seller's email or phone, so the model cannot leak them.
 * The buyer id comes from {@code @ToolMemoryId}, i.e. the user id typed in the UI.
 */
public class InterestTools {

    static final String QUILLMARK = "generated";

    private final JsonStore<Listing> listings;

    public InterestTools(JsonStore<Listing> listings) {
        this.listings = listings;
    }

    @Tool("Express the CURRENT user's interest in a listing. The platform notifies the seller and arranges the "
            + "meet-up; no contact details are shared.")
    public String expressInterest(@ToolMemoryId String buyerId, @P("listing ID, e.g. TX-0003") String listingId)
            throws RuleViolationException, DataAccessException {
        String id = listingId == null ? "" : listingId.trim();
        Listing l = listings.findWhere(x -> x.listingId().equalsIgnoreCase(id)).stream().findFirst()
                .orElseThrow(() -> new UnknownListingException(listingId));
        if (l.sellerId() != null && l.sellerId().equalsIgnoreCase(buyerId)) {
            return "Listing " + l.listingId() + " is your own listing.";
        }
        return "Interest in " + l.listingId() + " recorded for " + buyerId + ". The seller will be notified; the "
                + "meet-up is arranged through the platform at the Library Level 1 Book Swap Corner.";
    }
}
