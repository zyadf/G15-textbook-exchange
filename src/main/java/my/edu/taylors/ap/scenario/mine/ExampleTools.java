package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.HouseRules;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * WORKED EXAMPLE of tools. Run the app and ask "what time is it?" or "what are your rules?"
 * and watch the Agent trace panel: you will see the model request a tool, the tool run, and
 * the model use the result.
 * <p>
 * Your own tools go in a separate class (e.g. {@code KopitiamTools}). Keep tools THIN:
 * validate input, call your service, return short plain text.
 */
public class ExampleTools {

    private final GroupProfile profile;
    private final Clock clock;
    private final Path dataDir;

    public ExampleTools(GroupProfile profile, Clock clock, Path dataDir) {
        this.profile = profile;
        this.clock = clock;
        this.dataDir = dataDir;
    }

    @Tool("Current local date and time. Use it for anything that depends on the time of day or the weekday.")
    public String currentDateTime() {
        return LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("EEEE yyyy-MM-dd HH:mm", Locale.ENGLISH));
    }

    @Tool("The full text of this service's house rules.")
    public String houseRules() {
        return String.join("\n", HouseRules.forProfile(profile));
    }

    /**
     * Shows how a tool reads a JSON data file with JsonStore and how a checked exception
     * propagates (AssistantFactory turns it into a message for the model).
     * Generic Map records are used here only because this example does not know your scenario;
     * YOUR tools must use proper record classes.
     */
    @Tool("Count the records in one of the scenario's data files, e.g. menu.json")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String countRecords(@P("file name inside the data folder, e.g. menu.json") String fileName)
            throws DataAccessException {
        Path file = dataDir.resolve(Path.of(fileName).getFileName());   // blocks "../" tricks
        List<Map> rows = (List<Map>) (List) JsonStore.of(file, Map.class).findAll();
        return fileName + " has " + rows.size() + " records.";
    }
}
