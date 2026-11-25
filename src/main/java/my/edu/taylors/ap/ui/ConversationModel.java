package my.edu.taylors.ap.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;

/**
 * MODEL of the MVC triad: observable UI state only. The view binds to it; the controller changes it.
 * (Not to be confused with LangChain4j's ChatModel, which is the LLM.)
 */
public class ConversationModel {

    public enum Role { USER, AGENT, SYSTEM, ERROR }

    public record Entry(Role role, String text, LocalTime time) { }

    private final ObservableList<Entry> entries = FXCollections.observableArrayList();
    private final ObservableList<String> traceLines = FXCollections.observableArrayList();
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final StringProperty userId = new SimpleStringProperty("customer1");

    public ObservableList<Entry> entries() { return entries; }
    public ObservableList<String> traceLines() { return traceLines; }
    public BooleanProperty busyProperty() { return busy; }
    public StringProperty userIdProperty() { return userId; }

    public void add(Role role, String text) {
        entries.add(new Entry(role, text, LocalTime.now().withNano(0)));
    }

    /** Keeps the live trace panel short; the full trace is on disk. */
    public void addTrace(String line) {
        traceLines.add(line);
        if (traceLines.size() > 300) {
            traceLines.remove(0, traceLines.size() - 300);
        }
    }
}
