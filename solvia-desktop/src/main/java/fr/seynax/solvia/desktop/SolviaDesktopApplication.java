package fr.seynax.solvia.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SolviaDesktopApplication extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Solvia");
        BorderPane root = new BorderPane(title);

        stage.setTitle("Solvia");
        stage.setScene(new Scene(root, 960, 640));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
