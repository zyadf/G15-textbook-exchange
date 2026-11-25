// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

/**
 * One row of {@code listings.json}. The seed file in {@code data/} is never changed; the tools
 * read and write the live copy in {@code runtime/listings.json} (see JsonStore.seeded).
 * <p>
 * sellerEmail and sellerPhone are SYNTHETIC contact data that the agent must never reveal
 * (house rule T5). No tool in this package returns them.
 */
public record Listing(String listingId, String isbn, String sellerId, String sellerEmail, String sellerPhone,
                      String condition, long askingSen, String defects) {

    static final String QUILLMARK = "generated";
}
