package my.edu.taylors.ap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import my.edu.taylors.ap.config.AppConfig;
import my.edu.taylors.ap.ui.ChatController;
import my.edu.taylors.ap.ui.ChatView;
import my.edu.taylors.ap.ui.ConversationModel;


/** JavaFX entry point:  mvn javafx:run */
public class App extends Application {

    private AgentRuntime runtime;
    private ChatController controller;

    @Override
    public void start(Stage stage) {
        try {
            AppConfig config = AppConfig.load();
            runtime = AgentRuntime.start(config, config.clock());
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, e.getClass().getSimpleName() + ": " + e.getMessage());
            a.setHeaderText("The agent could not start");
            a.showAndWait();
            Platform.exit();
            return;
        }
        ConversationModel model = new ConversationModel();
        model.userIdProperty().set(runtime.module().defaultUserId());
        controller = new ChatController(model, runtime);
        ChatView view = new ChatView(model, controller, runtime.profile(),
                runtime.module().structuredActionLabel(), runtime.module().dataFiles());
        model.add(ConversationModel.Role.SYSTEM, "Connected to " + runtime.config().provider() + " / "
                + runtime.config().modelName() + ". Trace file: " + runtime.trace().currentFile());

        stage.setTitle("AP ASSIGNMENT - " + runtime.profile().scenario().title() + " - " + runtime.profile().groupCode());
        stage.setScene(new Scene(view.build(), 1366, 768));
        stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.shutdown();
        }
        if (runtime != null) {
            runtime.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
