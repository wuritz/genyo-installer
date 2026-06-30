package dev.genyo.installer.ui.tabs;

import dev.genyo.installer.InstallerOptions;
import dev.genyo.installer.LauncherType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

public class OptionsTab {

    private final Stage ownerStage;
    private final InstallerOptions options;
    private final Runnable onOptionsChanged;

    private final VBox root = new VBox(18);

    public OptionsTab(Stage ownerStage, InstallerOptions options, Runnable onOptionsChanged) {
        this.ownerStage = ownerStage;
        this.options = options;
        this.onOptionsChanged = onOptionsChanged;

        root.getStyleClass().add("options-tab");
        root.setPadding(new Insets(18));

        root.getChildren().addAll(buildInstallLocationSection(), buildInstallationProcessSection(), buildFooter());
    }

    public Region getView() {
        return root;
    }

    private Region buildInstallLocationSection() {
        Label sectionLabel = new Label("Install location");
        sectionLabel.getStyleClass().add("section-label");

        CheckBox cbOnlyLauncher = new CheckBox("Only install into this launcher:");
        cbOnlyLauncher.setSelected(options.explicitLauncher);
        Tooltip.install(cbOnlyLauncher, new Tooltip(
                "The installer only looks for the selected launcher's directories."));

        ComboBox<String> launcherCombo = new ComboBox<>();
        launcherCombo.getItems().addAll("Minecraft Launcher", "Prism Launcher");
        launcherCombo.getSelectionModel().select(
                options.selectedExplicitLauncher == LauncherType.PRISM ? 1 : 0);
        launcherCombo.disableProperty().bind(cbOnlyLauncher.selectedProperty().not());

        CheckBox cbSelectManually = new CheckBox("Manually select the install folder");
        Tooltip.install(cbSelectManually, new Tooltip(
                "Instead of the installer looking for folders, you decide where explicitly to install Genyo."));

        cbOnlyLauncher.selectedProperty().addListener((obs, was, isNow) -> {
            options.explicitLauncher = isNow;
            applyLauncherSelection(launcherCombo);
            onOptionsChanged.run();
        });

        launcherCombo.getSelectionModel().selectedIndexProperty().addListener((obs, was, isNow) -> {
            applyLauncherSelection(launcherCombo);
            onOptionsChanged.run();
        });

        cbSelectManually.selectedProperty().addListener((obs, was, isNow) -> {
            if (isNow) {
                boolean proceed = confirm(
                        "Note that enabling this completely skips any checks that ensure only valid install "
                                + "locations are used.\n\nDo you wish to proceed?");
                if (!proceed) {
                    cbSelectManually.setSelected(false);
                    return;
                }
            }
            options.manualInstallLocation = isNow;
        });

        HBox launcherRow = new HBox(8, cbOnlyLauncher, launcherCombo);
        launcherRow.setAlignment(Pos.CENTER_LEFT);

        // Apply initial state to InstallerOptions so the default (Prism Launcher,
        // "only this launcher" checked) matches what's shown, mirroring the
        // original UC_Options_Load.
        applyLauncherSelection(launcherCombo);

        return new VBox(10, sectionLabel, launcherRow, cbSelectManually);
    }

    private Region buildInstallationProcessSection() {
        Label sectionLabel = new Label("Installation process");
        sectionLabel.getStyleClass().add("section-label");

        CheckBox cbIgnore = new CheckBox("Ignore checks for Fabric and Meteor");
        cbIgnore.setSelected(options.ignoreFabricMeteor);
        Tooltip.install(cbIgnore, new Tooltip(
                "The installer blocks the download if it can't find Fabric or Meteor in your 'mods' folder. "
                        + "This ignores that check."));

        cbIgnore.selectedProperty().addListener((obs, was, isNow) -> options.ignoreFabricMeteor = isNow);

        return new VBox(10, sectionLabel, cbIgnore);
    }

    private Region buildFooter() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Hover on an option for more details.");
        hint.getStyleClass().add("hint-label");
        HBox hintRow = new HBox(hint);
        hintRow.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(spacer, hintRow);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void applyLauncherSelection(ComboBox<String> combo) {
        int index = combo.getSelectionModel().getSelectedIndex();
        options.selectedExplicitLauncher =
                index == 0 ? LauncherType.MINECRAFT : LauncherType.PRISM;
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation needed");
        alert.setHeaderText(null);
        alert.initOwner(ownerStage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

}
