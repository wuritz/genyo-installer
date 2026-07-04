package dev.genyo.installer.ui.pane;

import dev.genyo.installer.util.options.InstallerOptions;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

public class SettingsPane {

    private static final double WIDTH = 300;

    private final Stage ownerStage;
    private final InstallerOptions options;

    private final VBox root = new VBox(22);

    public SettingsPane(Stage ownerStage, InstallerOptions options) {
        this.ownerStage       = ownerStage;
        this.options          = options;

        root.getStyleClass().add("settings-pane");
        root.setPadding(new Insets(30, 26, 24, 24));
        root.setPrefWidth(WIDTH);
        root.setMinWidth(WIDTH);
        root.setMaxWidth(WIDTH);

        root.getChildren().addAll(
                buildHeader(),
                buildSection("INSTALL LOCATION", buildInstallLocationContent()),
                buildSection("INSTALLATION PROCESS", buildInstallationProcessContent()),
                buildFooter());
    }

    public Region getView() { return root; }

    // ---------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------

    private Region buildHeader() {
        Label gear = new Label("⚙");
        gear.getStyleClass().add("settings-gear");
        Label title = new Label("Settings");
        title.getStyleClass().add("settings-title");

        HBox row = new HBox(8, gear, title);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(row, new Insets(0, 0, 6, 0));
        return row;
    }

    private Region buildSection(String title, Region content) {
        Label header = new Label(title);
        header.getStyleClass().add("section-header");
        VBox card = new VBox(content);
        card.getStyleClass().add("option-group");
        return new VBox(8, header, card);
    }

    private Region buildInstallLocationContent() {
        // Version selector

        CheckBox cbManualVersion = new CheckBox("Manually select the version");
        cbManualVersion.setWrapText(true);
        Tooltip.install(cbManualVersion, new Tooltip(
                "Allows you to select what version of Genyo to install."));

        cbManualVersion.selectedProperty().addListener((obs, was, isNow) -> {
            options.manualVersionSelect = isNow;
        });

        // Install location selector

        CheckBox cbManualInstallLocation = new CheckBox("Manually select the install folder");
        cbManualInstallLocation.setWrapText(true);
        Tooltip.install(cbManualInstallLocation, new Tooltip(
                "You choose the exact folder to install into, skipping automatic detection."));

        cbManualInstallLocation.selectedProperty().addListener((obs, was, isNow) -> {
            if (isNow) {
                boolean proceed = confirm(
                        "Enabling this skips all checks that validate the install location.\n\nDo you want to proceed?");
                if (!proceed) {
                    cbManualInstallLocation.setSelected(false);
                    return;
                }
            }
            options.manualInstallLocation = isNow;
        });

        // Assemble

        return new VBox(10, cbManualVersion, cbManualInstallLocation);
    }

    private Region buildInstallationProcessContent() {
        CheckBox cbIgnore = new CheckBox("Ignore checks for Fabric and Meteor");
        cbIgnore.setWrapText(true);
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
        hint.setWrapText(true);

        VBox footer = new VBox(spacer, hint);
        VBox.setVgrow(footer, Priority.ALWAYS);
        return footer;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.initOwner(ownerStage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}