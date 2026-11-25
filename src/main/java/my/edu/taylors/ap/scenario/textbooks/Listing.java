package my.edu.taylors.ap.scenario.textbooks;

/**
 * Row of data/listings.json (seed) / runtime/listings.json. Contact fields are SYNTHETIC test
 * data that the agent must never reveal (house rule T5).
 */
public record Listing(String listingId, String isbn, String sellerId, String sellerEmail, String sellerPhone,
                      String condition, long askingSen, String defects) { }
