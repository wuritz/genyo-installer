package dev.genyo.installer;

import dev.genyo.installer.ui.InstallerTab;
import dev.genyo.installer.ui.OptionsTab;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Java port of {@code Form1.cs} / {@code Form1.Designer.cs}: the main
 * window, hosting the "Installer" and "Options" tabs plus the bottom-left
 * "Installer version:" label.
 */
public class MainApp extends Application {

    public static final String APP_VERSION = "1.0.0";

    @Override
    public void start(Stage stage) {
        InstallerOptions options = new InstallerOptions(APP_VERSION);

        InstallerTab installerTab = new InstallerTab(stage, options, getHostServices());
        OptionsTab optionsTab = new OptionsTab(stage, options, installerTab::refreshLabels);

        Tab installerTabUi = new Tab("Installer", installerTab.getView());
        installerTabUi.setClosable(false);

        Tab optionsTabUi = new Tab("Options", optionsTab.getView());
        optionsTabUi.setClosable(false);

        TabPane tabPane = new TabPane(installerTabUi, optionsTabUi);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Label versionLabel = new Label("Installer version:  v" + APP_VERSION);
        versionLabel.getStyleClass().add("footer-label");
        HBox footer = new HBox(versionLabel);
        footer.getStyleClass().add("footer-bar");

        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(footer);

        Scene scene = new Scene(root, 660, 430);
        scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());

        stage.setTitle("Genyo Installer");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/genyo512.png")));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}