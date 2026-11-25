package my.edu.taylors.ap.scenario;

import dev.langchain4j.model.chat.ChatModel;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.scenario.grooming.GroomingModule;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamModule;
//import my.edu.taylors.ap.scenario.mine.MyScenarioModule;
import my.edu.taylors.ap.scenario.makerspace.MakerspaceModule;
import my.edu.taylors.ap.scenario.rooms.RoomsModule;
import my.edu.taylors.ap.scenario.textbooks.TextbooksModule;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;

/** Creates your group's scenario module. You do not need to change this class. */
public final class ScenarioModules {

    private ScenarioModules() { }

    public static ScenarioModule create(GroupProfile profile, Path scenarioDir, Path runtimeDir, ChatModel model,
                                        TraceLogger trace, Clock clock) throws DataAccessException {
        //return new MyScenarioModule(profile, scenarioDir.resolve("data"), runtimeDir, model, trace, clock);

        Path data = scenarioDir.resolve("data");
        return switch (profile.scenario()) {
            case KOPITIAM -> new KopitiamModule(profile, data, runtimeDir, model, trace, clock);
            case MAKERSPACE -> new MakerspaceModule(profile, data, runtimeDir, model, trace, clock);
            case ROOMS -> new RoomsModule(profile, data, runtimeDir, model, trace, clock);
            case GROOMING -> new GroomingModule(profile, data, runtimeDir, model, trace, clock);
            case TEXTBOOKS -> new TextbooksModule(profile, data, runtimeDir, model, trace, clock);
        };
    }
}
