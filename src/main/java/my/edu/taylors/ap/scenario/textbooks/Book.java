package my.edu.taylors.ap.scenario.textbooks;

/** Row of data/books.json. supersededBy = ISBN of the newer edition, or null if this is current. */
public record Book(String isbn, String title, int edition, int year, int listPriceSen, String supersededBy) {

    public boolean superseded() {
        return supersededBy != null && !supersededBy.isBlank();
    }
}
