package my.edu.taylors.ap.config;

/**
 * The five business scenarios used in ITS66704 AT2.
 * A group's scenario is fixed by its group number (see {@link GroupSeeder}).
 */
public enum Scenario {
    KOPITIAM(1, "Kopitiam Ordering Agent", "s1-kopitiam"),
    MAKERSPACE(2, "Makerspace Equipment Loans Agent", "s2-makerspace"),
    ROOMS(3, "Campus Room Booking Agent", "s3-rooms"),
    GROOMING(4, "Pet Grooming Scheduler Agent", "s4-grooming"),
    TEXTBOOKS(5, "Textbook Exchange Agent", "s5-textbooks");

    private final int id;
    private final String title;
    private final String folder;

    Scenario(int id, String title, String folder) {
        this.id = id;
        this.title = title;
        this.folder = folder;
    }

    public int id() { return id; }
    public String title() { return title; }
    /** Folder under {@code scenarios/} holding this scenario's data/ and docs/. */
    public String folder() { return folder; }
}
