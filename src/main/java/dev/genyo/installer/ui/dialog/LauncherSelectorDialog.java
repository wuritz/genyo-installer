package dev.genyo.installer.ui.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;

public class LauncherSelectorDialog {

    private final Stage stage;
    private boolean isMLauncherSelected = false;

    public LauncherSelectorDialog(Window owner) {
        stage = new Stage();
        stage.setTitle("Select your launcher");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);

        Button btnMinecraftLauncher = new Button("Minecraft Launcher");
        btnMinecraftLauncher.setOnAction(e -> {
            isMLauncherSelected = true;
            stage.close();
        });
        btnMinecraftLauncher.setMaxWidth(Double.MAX_VALUE);
        btnMinecraftLauncher.getStyleClass().add("launcher-selector-btn");

        VBox root = getVBox(btnMinecraftLauncher);
        root.getStyleClass().add("launcher-selector-root");

        Scene scene = new Scene(root, 400, 200);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style/style.css")).toExternalForm());
        stage.setScene(scene);
    }

    private VBox getVBox(Button btnMinecraftLauncher) {
        Button btnPrismLauncher = new Button("Prism Launcher");
        btnPrismLauncher.getStyleClass().add("prism-button");
        btnPrismLauncher.setOnAction(e -> {
            isMLauncherSelected = false;
            stage.close();
        });
        btnPrismLauncher.setMaxWidth(Double.MAX_VALUE);
        btnPrismLauncher.getStyleClass().add("launcher-selector-btn");

        Label labelFirst = new Label("Select the launcher type you want to install Genyo to.");
        labelFirst.getStyleClass().add("label-launcher-selector-first");

        Label labelManualInstall = new Label("If you don't see your launcher type here, then you should "
                + "instead select an install location manually by checking that option in the Options tab.");
        labelManualInstall.getStyleClass().add("launcher-selector-manual-install");
        labelManualInstall.setWrapText(true);

        VBox boxButtons = new VBox(8, btnPrismLauncher, btnMinecraftLauncher);
        boxButtons.getStyleClass().add("launcher-selector-buttons");
        boxButtons.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, labelFirst, boxButtons, labelManualInstall);
        root.setPadding(new Insets(15));
        return root;
    }

    public Optional<Boolean> showAndWaitForSelection() {
        stage.showAndWait();
        return Optional.of(isMLauncherSelected);
    }

}
