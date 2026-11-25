package my.edu.taylors.ap.scenario.grooming;

/** Row of data/services.json: a grooming service (named GroomService to avoid confusion with service classes). */
public record GroomService(String code, String name, int baseMinutes, int basePriceSen) { }
