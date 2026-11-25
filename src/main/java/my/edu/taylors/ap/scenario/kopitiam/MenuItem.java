package my.edu.taylors.ap.scenario.kopitiam;

import java.util.List;

/** One row of {@code scenarios/s1-kopitiam/data/menu.json}. */
public record MenuItem(String code, String name, String category, int priceSen, List<String> allergens, boolean available) {

    public boolean isDrink() {
        return "DRINK".equalsIgnoreCase(category);
    }
}
