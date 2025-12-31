package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;
    private SimpleClient client;

    @Override
    public void start(Stage stage) throws IOException {

        //  Load UI first so controllers exist and can register to EventBus
        scene = new Scene(loadFXML("primary"), 640, 480);


        stage.setScene(scene);
        stage.show();


        EventBus.getDefault().register(this);

        // 3) Now connect to server
        client = SimpleClient.getClient();
        client.openConnection();

        //  Subscribe on server side after connection is open
        try {
            client.sendToServer("add client");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    @Override
    public void stop() throws Exception {
        try {
            // Unregister App from EventBus
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this);
            }

            // Close server connection gracefully
            if (client != null) {
                try {
                    client.sendToServer("remove client");
                } catch (IOException ignored) {
                    // not critical on shutdown
                }
                client.closeConnection();
            }
        } finally {
            super.stop();
        }
    }

    /**
     * WarningEvent handler (demo for EventBus).
     * Game events are handled inside PrimaryController now.
     */
    @Subscribe
    public void onWarningEvent(WarningEvent event) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.WARNING,
                    String.format("Message: %s\nTimestamp: %s\n",
                            event.getWarning().getMessage(),
                            event.getWarning().getTime().toString())
            );
            alert.show();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
