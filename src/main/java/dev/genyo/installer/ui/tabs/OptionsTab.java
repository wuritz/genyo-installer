package dev.genyo.installer.ui;

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

/**
 * Java port of {@code UC_Options.cs} / {@code UC_Options.Designer.cs}: the
 * "Options" tab with install-location and installation-process settings.
 */
public class OptionsTab {

    private final Stage ownerStage;
    private final InstallerOptions options;
    private final Runnable onOptionsChanged;

    private final VBox root = new VBox(20);

    public OptionsTab(Stage ownerStage, InstallerOptions options, Runnable onOptionsChanged) {
        this.ownerStage       = ownerStage;
        this.options          = options;
        this.onOptionsChanged = onOptionsChanged;

        root.getStyleClass().add("options-tab");

        root.getChildren().addAll(
                buildSection("INSTALL LOCATION",   buildInstallLocationContent()),
                buildSection("INSTALLATION PROCESS", buildInstallationProcessContent()),
                buildFooter());
    }

    public Region getView() { return root; }

    // ---------------------------------------------------------------
    // Layout helpers
    // ---------------------------------------------------------------

    /** Wraps content in a section with a header label + card-style box. */
    private Region buildSection(String title, Region content) {
        Label header = new Label(title);
        header.getStyleClass().add("section-header");

        VBox card = new VBox(content);
        card.getStyleClass().add("option-group");

        return new VBox(4, header, card);
    }

    private Region buildInstallLocationContent() {
        CheckBox cbOnlyLauncher = new CheckBox("Only install into this launcher:");
        cbOnlyLauncher.setSelected(options.explicitLauncher);
        Tooltip.install(cbOnlyLauncher, new Tooltip(
                "The installer only searches the selected launcher's directories."));

        ComboBox<String> launcherCombo = new ComboBox<>();
        launcherCombo.getItems().addAll("Minecraft Launcher", "Prism Launcher");
        launcherCombo.getSelectionModel().select(
                options.selectedExplicitLauncher == LauncherType.PRISM ? 1 : 0);
        launcherCombo.disableProperty().bind(cbOnlyLauncher.selectedProperty().not());
        launcherCombo.setPrefWidth(170);

        CheckBox cbSelectManually = new CheckBox("Manually select the install folder");
        Tooltip.install(cbSelectManually, new Tooltip(
                "You choose the exact folder to install into, skipping automatic detection."));

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
                        "Enabling this skips all checks that validate the install location.\n\nDo you want to proceed?");
                if (!proceed) {
                    cbSelectManually.setSelected(false);
                    return;
                }
            }
            options.manualInstallLocation = isNow;
        });

        applyLauncherSelection(launcherCombo);

        HBox launcherRow = new HBox(10, cbOnlyLauncher, launcherCombo);
        launcherRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, launcherRow, cbSelectManually);
        return box;
    }

    private Region buildInstallationProcessContent() {
        CheckBox cbIgnore = new CheckBox("Ignore checks for Fabric and Meteor");
        cbIgnore.setSelected(options.ignoreFabricMeteor);
        Tooltip.install(cbIgnore, new Tooltip(
                "Normally the installer requires Fabric and Meteor to already be in your mods folder. "
                        + "This skips that check."));
        cbIgnore.selectedProperty().addListener((obs, was, isNow) -> options.ignoreFabricMeteor = isNow);

        return new VBox(cbIgnore);
    }

    private Region buildFooter() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Hover over an option for details.");
        hint.getStyleClass().add("hint-label");

        HBox hintRow = new HBox(hint);
        hintRow.setAlignment(Pos.BOTTOM_RIGHT);

        VBox footer = new VBox(spacer, hintRow);
        VBox.setVgrow(footer, Priority.ALWAYS);
        return footer;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void applyLauncherSelection(ComboBox<String> combo) {
        options.selectedExplicitLauncher =
                combo.getSelectionModel().getSelectedIndex() == 0
                        ? LauncherType.MINECRAFT
                        : LauncherType.PRISM;
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.initOwner(ownerStage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}