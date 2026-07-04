package dev.genyo.installer;

import dev.genyo.installer.api.options.InstallerOptions;
import dev.genyo.installer.ui.tabs.InstallerTab;
import dev.genyo.installer.ui.tabs.OptionsTab;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    public static final String APP_VERSION = "1.0.0";

    @Override
    public void start(Stage stage) {
        TabPane tabPane = getPane(stage);

        Label versionLabel = new Label("Installer version:  v" + APP_VERSION);
        versionLabel.getStyleClass().add("footer-label");
        HBox footer = new HBox(versionLabel);
        footer.getStyleClass().add("footer-bar");

        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(footer);

        Scene scene = new Scene(root, 660, 430);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style/style.css")).toExternalForm());

        stage.setTitle("Genyo Installer");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/genyo512.png"))));
        stage.show();
    }

    private TabPane getPane(Stage stage) {
        InstallerOptions options = new InstallerOptions(APP_VERSION);

        InstallerTab installerTab = new InstallerTab(stage, options, getHostServices());
        OptionsTab optionsTab = new OptionsTab(stage, options);

        Tab installerTabUi = new Tab("Installer", installerTab.getView());
        installerTabUi.setClosable(false);

        Tab optionsTabUi = new Tab("Options", optionsTab.getView());
        optionsTabUi.setClosable(false);

        TabPane tabPane = new TabPane(installerTabUi, optionsTabUi);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabPane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}