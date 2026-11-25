package my.edu.taylors.ap.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.HouseRules;
import my.edu.taylors.ap.ui.ConversationModel.Entry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * VIEW: builds the scene graph and binds it to the {@link ConversationModel}.
 * Contains no business logic: every user action is forwarded to the {@link ChatController}.
 */
public class ChatView {

    private final ConversationModel model;
    private final ChatController controller;
    private final GroupProfile profile;
    private final String structuredLabel;
    private final List<Path> dataFiles;

    public ChatView(ConversationModel model, ChatController controller, GroupProfile profile,
                    String structuredLabel, List<Path> dataFiles) {
        this.model = model;
        this.controller = controller;
        this.profile = profile;
        this.structuredLabel = structuredLabel;
        this.dataFiles = dataFiles;
    }

    public Parent build() {
        controller.setConfirmer(this::confirm);

        // --- header ---------------------------------------------------------------
        Label title = new Label(profile.scenario().title());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label group = new Label(profile.groupCode());
        group.setStyle("-fx-text-fill: #666;");
        TextField userField = new TextField();
        userField.textProperty().bindBidirectional(model.userIdProperty());
        userField.setPrefColumnCount(10);
        Button rules = new Button("House rules");
        rules.setOnAction(e -> showRules());
        Button clear = new Button("Clear memory");
        clear.setOnAction(e -> controller.onClearMemory());
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, group, spacer, new Label("User:"), userField, rules, clear);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));

        // --- conversation ---------------------------------------------------------
        ListView<Entry> chat = new ListView<>(model.entries());
        chat.setCellFactory(lv -> new EntryCell());
        model.entries().addListener((javafx.collections.ListChangeListener<Entry>) c ->
                chat.scrollTo(model.entries().size() - 1));

        // --- live trace panel -----------------------------------------------------
        ListView<String> trace = new ListView<>(model.traceLines());
        trace.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        model.traceLines().addListener((javafx.collections.ListChangeListener<String>) c ->
                trace.scrollTo(model.traceLines().size() - 1));

        // --- data tab: the JSON files the tools read and write ---------------------
        ComboBox<Path> files = new ComboBox<>(FXCollections.observableArrayList(dataFiles));
        TextArea fileView = new TextArea();
        fileView.setEditable(false);
        fileView.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        Runnable refresh = () -> fileView.setText(readQuietly(files.getValue()));
        files.setOnAction(e -> refresh.run());
        Button reload = new Button("Refresh");
        reload.setOnAction(e -> refresh.run());
        if (!dataFiles.isEmpty()) {
            files.setValue(dataFiles.get(0));
            refresh.run();
        }
        controller.setAfterEachCall(refresh);       // tools may have changed the files
        VBox dataPane = new VBox(6, new HBox(6, files, reload), fileView);
        VBox.setVgrow(fileView, Priority.ALWAYS);

        TabPane side = new TabPane(new Tab("Agent trace", trace), new Tab("Data", dataPane));
        side.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        SplitPane split = new SplitPane(chat, side);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.62);

        // --- input bar ------------------------------------------------------------
        TextField input = new TextField();
        input.setPromptText("Type a message and press Enter");
        HBox.setHgrow(input, Priority.ALWAYS);
        Button send = new Button("Send");
        Button structured = new Button(structuredLabel);
        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> controller.onCancel());
        cancel.visibleProperty().bind(model.busyProperty());
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(22, 22);
        spinner.visibleProperty().bind(model.busyProperty());
        send.disableProperty().bind(model.busyProperty());
        structured.disableProperty().bind(model.busyProperty());

        send.setOnAction(e -> { controller.onSend(input.getText()); input.clear(); });
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                controller.onSend(input.getText());
                input.clear();
            }
        });
        structured.setOnAction(e -> { controller.onStructured(input.getText()); input.clear(); });

        HBox inputBar = new HBox(8, input, spinner, cancel, send, structured);
        inputBar.setAlignment(Pos.CENTER_LEFT);
        inputBar.setPadding(new Insets(10));

        BorderPane root = new BorderPane(split, header, null, inputBar, null);
        BorderPane.setMargin(split, new Insets(0, 10, 0, 10));

        // watermark sits ABOVE the whole UI
        return new StackPane(root, Watermark.overlay(profile));
    }

    /** Human-in-the-loop: nothing structured is committed without an explicit OK. */
    private boolean confirm(String summary) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm");
        a.setHeaderText("Commit this request?");
        TextArea text = new TextArea(summary);
        text.setWrapText(true);
        text.setEditable(false);
        text.setPrefSize(520, 240);
        a.getDialogPane().setContent(text);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static String readQuietly(Path file) {
        if (file == null) {
            return "";
        }
        try {
            return Files.exists(file) ? Files.readString(file) : "(file not created yet: " + file + ")";
        } catch (IOException e) {
            return "Cannot read " + file + ": " + e.getMessage();
        }
    }

    private void showRules() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("House rules - " + profile.groupCode());
        a.setHeaderText(profile.scenario().title() + "  (" + profile.watermarkId() + ")");
        TextArea text = new TextArea(String.join("\n\n", HouseRules.forProfile(profile)));
        text.setWrapText(true);
        text.setEditable(false);
        text.setPrefSize(560, 320);
        a.getDialogPane().setContent(text);
        a.showAndWait();
    }

    /** Renders one conversation entry with a role-specific style. */
    private static final class EntryCell extends ListCell<Entry> {
        @Override
        protected void updateItem(Entry e, boolean empty) {
            super.updateItem(e, empty);
            if (empty || e == null) {
                setText(null);
                setStyle("");
                return;
            }
            setWrapText(true);
            setPrefWidth(0);    // lets wrapping follow the ListView width
            setText("[" + e.time() + "] " + e.role() + ":\n" + e.text());
            setStyle(switch (e.role()) {
                case USER -> "-fx-background-color: #eef4ff;";
                case AGENT -> "-fx-background-color: #ffffff;";
                case SYSTEM -> "-fx-background-color: #f4f4f4; -fx-font-family: monospace;";
                case ERROR -> "-fx-background-color: #fdecea; -fx-text-fill: #8a1c12;";
            });
        }
    }
}
