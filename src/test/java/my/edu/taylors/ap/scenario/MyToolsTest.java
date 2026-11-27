// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario;

import my.edu.taylors.ap.config.GroupSeeder;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.mine.Book;
import my.edu.taylors.ap.scenario.mine.BuyerTools;
import my.edu.taylors.ap.scenario.mine.ExchangeExceptions;
import my.edu.taylors.ap.scenario.mine.ExchangeRules;
import my.edu.taylors.ap.scenario.mine.ExchangeService;
import my.edu.taylors.ap.scenario.mine.InterestTools;
import my.edu.taylors.ap.scenario.mine.Listing;
import my.edu.taylors.ap.scenario.mine.ListingTools;
import my.edu.taylors.ap.scenario.mine.PriceTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The tools against a temporary listings.json: IDs, per-user isolation and no contact details (T5). */
class MyToolsTest {

    static final String QUILLMARK = "generated";

    static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    static final Book JAVA_12 = new Book("9780000000101", "Introduction to Java Programming and Data Structures", 12, 2021, 25900, null);

    @TempDir
    Path tmp;
    JsonStore<Listing> store;
    ListingTools listingTools;
    BuyerTools buyerTools;
    PriceTools priceTools;
    InterestTools interestTools;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(LocalDateTime.parse("2026-11-02T10:00").atZone(KL).toInstant(), KL);
        ExchangeService service = new ExchangeService(List.of(JAVA_12), ExchangeRules.from(GroupSeeder.profileFor(15)), clock);
        store = JsonStore.of(tmp.resolve("listings.json"), Listing.class);
        store.saveAll(List.of(new Listing("TX-0001", "9780000000101", "STU-145", "hui.min.lee@example.com",
                "012-345 6789", "B", 14000, "")));
        listingTools = new ListingTools(service, store);
        buyerTools = new BuyerTools(service, store);
        priceTools = new PriceTools(service);
        interestTools = new InterestTools(store);
    }

    @Test
    void createListingIssuesNextIdAndPersists() throws Exception {
        String reply = listingTools.createListing("STU-999", "9780000000101", "c", "RM90", "");
        assertTrue(reply.contains("TX-0002"), reply);
        Listing saved = store.findAll().get(1);
        assertEquals("STU-999", saved.sellerId());
        assertEquals("C", saved.condition());
        assertEquals(9000, saved.askingSen());
    }

    @Test
    void createListingEnforcesRulesAndWritesNothing() throws Exception {
        assertThrows(ExchangeExceptions.OverpricedListingException.class,
                () -> listingTools.createListing("STU-999", "9780000000101", "B", "164.41", ""));
        assertThrows(ExchangeExceptions.MissingDefectsException.class,
                () -> listingTools.createListing("STU-999", "9780000000101", "D", "40", ""));
        assertEquals(1, store.findAll().size());
    }

    @Test
    void myListingsShowsOnlyTheCurrentUser() throws Exception {
        assertTrue(listingTools.myListings("STU-145").contains("TX-0001"));
        assertEquals("You have no listings.", listingTools.myListings("STU-999"));
    }

    @Test
    void buyerToolsShowT3PriceButNeverContactDetails() throws Exception {
        String out = buyerTools.findListings("9780000000101");
        assertTrue(out.contains("buyer pays RM147.00"), out);          // 140.00 + 5%
        assertFalse(out.contains("@"), out);
        assertFalse(out.contains("012-345"), out);
        assertThrows(ExchangeExceptions.UnknownListingException.class, () -> buyerTools.buyerQuote("TX-9999"));
    }

    @Test
    void priceToolGivesT1SuggestionAndT2Limit() throws Exception {
        String out = priceTools.suggestPrice("978-0-00-000010-1", "B");
        assertTrue(out.contains("RM137.00"), out);                     // T1 worked example
        assertTrue(out.contains("RM164.40"), out);                     // T2: 137.00 x 1.20
    }

    @Test
    void interestToolNeverSharesContactDetails() throws Exception {
        String out = interestTools.expressInterest("STU-999", "tx-0001");
        assertTrue(out.contains("TX-0001"), out);
        assertFalse(out.contains("@"), out);
        assertFalse(out.contains("012-345"), out);
        assertTrue(interestTools.expressInterest("STU-145", "TX-0001").contains("your own listing"));
        assertThrows(ExchangeExceptions.UnknownListingException.class,
                () -> interestTools.expressInterest("STU-999", "TX-9999"));
    }
}
