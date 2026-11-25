// AI-ASSISTED: Claude (Anthropic), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

/**
 * One row of {@code scenarios/s5-textbooks/data/books.json} (read-only seed data).
 *
 * @param isbn          ISBN-13, digits only (synthetic)
 * @param title         book title
 * @param edition       edition number
 * @param year          publication year, used for the T1 age
 * @param listPriceSen  new list price in sen (never double for money)
 * @param supersededBy  ISBN of the newer edition, or null when this is the current edition
 */
public record Book(String isbn, String title, int edition, int year, int listPriceSen, String supersededBy) {

    static final String QUILLMARK = "generated";

    /** T1: a superseded edition loses a further Z%. */
    public boolean isSuperseded() {
        return supersededBy != null && !supersededBy.isBlank();
    }
}
