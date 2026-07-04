package dev.genyo.installer;

import dev.genyo.installer.util.options.InstallerOptions;
import dev.genyo.installer.ui.pane.HeroPane;
import dev.genyo.installer.ui.pane.SettingsPane;
import dev.genyo.installer.util.ResourceReference;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    public static final String APP_VERSION = "2.1.0";

    @Override
    public void start(Stage stage) {
        InstallerOptions options = new InstallerOptions(APP_VERSION);

        HeroPane heroPane = new HeroPane(stage, options, getHostServices());
        SettingsPane settingsPane = new SettingsPane(stage, options);

        Separator divider = new Separator(Orientation.VERTICAL);
        divider.getStyleClass().add("pane-divider");

        HBox content = new HBox(heroPane.getView(), divider, settingsPane.getView());

        Label versionLabel = new Label("Installer version:  v" + APP_VERSION);
        versionLabel.getStyleClass().add("footer-label");
        HBox footer = new HBox(versionLabel);
        footer.getStyleClass().add("footer-bar");

        BorderPane root = new BorderPane();
        root.setCenter(content);
        root.setBottom(footer);

        Scene scene = new Scene(root, 720, 480);
        scene.getStylesheets().add(ResourceReference.STYLE_RES);

        stage.setTitle("Genyo Installer");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(ResourceReference.IMG512_RES));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}