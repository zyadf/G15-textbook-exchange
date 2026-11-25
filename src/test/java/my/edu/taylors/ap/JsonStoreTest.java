package my.edu.taylors.ap;

import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.kopitiam.MenuItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonStoreTest {

    /** Any record works; your scenario has its own. */
    record MenuItem(String code, String name, String category, int priceSen, List<String> allergens, boolean available) { }

    @Test
    void roundTripsRecordsThroughJson() throws Exception {
        Path f = Files.createTempDirectory("js").resolve("menu.json");
        JsonStore<MenuItem> store = JsonStore.of(f, MenuItem.class);
        assertTrue(store.findAll().isEmpty());
        store.append(new MenuItem("D01", "Teh Tarik", "DRINK", 280, List.of("dairy"), true));
        store.append(new MenuItem("D02", "Kopi O", "DRINK", 220, List.of(), true));
        List<MenuItem> back = JsonStore.of(f, MenuItem.class).findAll();
        assertEquals(2, back.size());
        assertEquals(new MenuItem("D01", "Teh Tarik", "DRINK", 280, List.of("dairy"), true), back.get(0));
    }

    @Test
    void corruptFileRaisesCheckedException() throws Exception {
        Path f = Files.createTempDirectory("js").resolve("bad.json");
        Files.writeString(f, "[{\"code\": \"D01\", ");
        assertThrows(DataAccessException.class, () -> JsonStore.of(f, MenuItem.class).findAll());
    }

    @Test
    void seededStoreCopiesSeedOnceAndNeverTouchesIt() throws Exception {
        Path dir = Files.createTempDirectory("js");
        Path seed = dir.resolve("seed.json");
        Files.writeString(seed, "[{\"code\":\"D01\",\"name\":\"Teh Tarik\",\"category\":\"DRINK\",\"priceSen\":280,\"allergens\":[],\"available\":true}]");
        JsonStore<MenuItem> live = JsonStore.seeded(seed, dir.resolve("runtime/live.json"), MenuItem.class);
        live.append(new MenuItem("D02", "Kopi O", "DRINK", 220, List.of(), true));
        assertEquals(2, live.findAll().size());
        assertEquals(1, JsonStore.of(seed, MenuItem.class).findAll().size());
    }

    // TODO [A6] add a test that YOUR scenario's data files parse into YOUR record classes.
    @Test
    void scenarioSeedDataParses() throws Exception {
        assertEquals(18, JsonStore.of(Path.of("scenarios/s1-kopitiam/data/menu.json"), my.edu.taylors.ap.scenario.kopitiam.MenuItem.class).findAll().size());
    }
}
