package my.edu.taylors.ap.scenario.grooming;

/** Row of data/pets.json (synthetic). visits = number of completed visits; 0 means first visit. */
public record Pet(String petId, String name, String breed, String ownerId, int visits) { }
