package dev.genyo.installer.ui.tabs;

import dev.genyo.installer.api.options.InstallerOptions;
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

public class OptionsTab {

    private final Stage ownerStage;
    private final InstallerOptions options;

    private final VBox root = new VBox(20);

    public OptionsTab(Stage ownerStage, InstallerOptions options) {
        this.ownerStage       = ownerStage;
        this.options          = options;

        root.getStyleClass().add("options-tab");

        root.getChildren().addAll(
                buildSection("Install location",   buildInstallLocationContent()),
                buildSection("Installation process", buildInstallationProcessContent()),
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
        CheckBox cbManualVersion =  new CheckBox("Manually select the version to be installed.");
        cbManualVersion.setSelected(options.manualVersionSelect);
        Tooltip.install(cbManualVersion, new Tooltip(
                "Allows you to select what version you want to install, instead of the latest one"
        ));

        CheckBox cbManualInstallLocation = new CheckBox("Manually select the install folder");
        Tooltip.install(cbManualInstallLocation, new Tooltip(
                "You choose the exact folder to install into, skipping automatic detection."));

        // Listener add

        cbManualVersion.selectedProperty().addListener((obs, was, isNow) -> {
           options.manualVersionSelect = isNow;
        });

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

        VBox box = new VBox(12, cbManualVersion, cbManualInstallLocation);
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

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.initOwner(ownerStage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}