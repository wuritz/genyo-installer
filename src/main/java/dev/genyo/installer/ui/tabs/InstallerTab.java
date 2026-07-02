package dev.genyo.installer.ui;

import dev.genyo.installer.InstallerOptions;
import dev.genyo.installer.api.InstallerService;
import dev.genyo.installer.net.GitHubReleaseClient;
import dev.genyo.installer.path.PathSearcher;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java port of {@code UC_Installer.cs} / {@code UC_Installer.Designer.cs}:
 * the "Installer" tab showing the Genyo logo, installed/latest version,
 * quick links, and the Install button.
 */
public class InstallerTab {

    private final Stage ownerStage;
    private final InstallerOptions options;
    private final HostServices hostServices;
    private final GitHubReleaseClient releaseClient;

    private final Label installedVersionValue = new Label("—");
    private final Label latestVersionValue    = new Label("Fetching...");
    private final Label statusLabel           = new Label("");
    private final Button installButton        = new Button("Install Genyo");

    private final BorderPane root = new BorderPane();

    public InstallerTab(Stage ownerStage, InstallerOptions options, HostServices hostServices) {
        this.ownerStage   = ownerStage;
        this.options      = options;
        this.hostServices = hostServices;
        this.releaseClient = new GitHubReleaseClient(options.installerVersion);

        root.getStyleClass().add("installer-tab");

        root.setTop(buildHeader());
        root.setCenter(buildVersionInfo());
        root.setRight(buildRightPanel());

        refreshLabels();
    }

    public Region getView() { return root; }

    // ---------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------

    private Region buildHeader() {
        ImageView logo = new ImageView(new Image(
                getClass().getResourceAsStream("/images/genyo512.png")));
        logo.setFitWidth(64);
        logo.setFitHeight(64);
        logo.setPreserveRatio(true);

        Label title = new Label("Genyo Addon");
        title.getStyleClass().add("app-title");

        // Link buttons sit on the right of the header row
        HBox links = buildLinkButtons();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, logo, title, spacer, links);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 0, 0));

        Separator sep = new Separator();
        sep.setPadding(new Insets(14, 0, 14, 0));

        VBox top = new VBox(header, sep);
        return top;
    }

    private Region buildVersionInfo() {
        Label installedLabel = new Label("INSTALLED");
        installedLabel.getStyleClass().add("version-label");
        installedVersionValue.getStyleClass().add("version-value");

        Label latestLabel = new Label("LATEST");
        latestLabel.getStyleClass().add("version-label");
        latestVersionValue.getStyleClass().add("version-value");

        Button changelogs = new Button("View changelogs →");
        changelogs.getStyleClass().add("changelog-button");
        changelogs.setOnAction(e -> openBrowser("https://genyo.dev/changelogs"));

        Region spacer = new Region();
        spacer.setPrefHeight(10);

        VBox box = new VBox(4,
                installedLabel, installedVersionValue,
                spacer,
                latestLabel, latestVersionValue,
                changelogs);
        box.setAlignment(Pos.TOP_LEFT);
        BorderPane.setMargin(box, new Insets(0, 16, 0, 0));
        return box;
    }

    private HBox buildLinkButtons() {
        Button github  = linkButton("GitHub",  "https://github.com/wuritz/genyo-addon");
        Button website = linkButton("Website", "https://genyo.dev");
        Button discord = linkButton("Discord", "https://genyo.dev/discord");

        for (Button b : List.of(github, website, discord)) {
            b.setPrefWidth(84);
        }

        HBox box = new HBox(8, github, website, discord);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private Button linkButton(String text, String url) {
        Button b = new Button(text);
        b.getStyleClass().add("link-button");
        b.setOnAction(e -> openBrowser(url));
        return b;
    }

    /** Right panel: status label + install button, bottom-aligned. */
    private Region buildRightPanel() {
        statusLabel.getStyleClass().add("status-label");

        installButton.getStyleClass().add("install-button");
        installButton.setPrefWidth(188);
        installButton.setOnAction(e -> onInstallClicked());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox box = new VBox(8, spacer, statusLabel, installButton);
        box.setAlignment(Pos.BOTTOM_RIGHT);
        BorderPane.setMargin(box, new Insets(0, 0, 0, 0));
        return box;
    }

    // ---------------------------------------------------------------
    // Behavior
    // ---------------------------------------------------------------

    private void onInstallClicked() {
        if (options.installing) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Currently installing.", ButtonType.OK);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.initOwner(ownerStage);
            alert.showAndWait();
            return;
        }
        InstallerService service = new InstallerService(ownerStage, options, this::refreshLabels);
        service.startInstalling();
    }

    private void openBrowser(String url) {
        try {
            hostServices.showDocument(url);
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not open the browser.", ButtonType.OK);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.initOwner(ownerStage);
            alert.showAndWait();
        }
    }

    public void refreshLabels() {
        Map<String, Integer> installedVersionCounts = scanInstalledVersions();
        installedVersionValue.setText(formatInstalledVersions(installedVersionCounts));

        latestVersionValue.setText("Fetching...");
        setStatus("", null);

        Task<String> fetchLatest = new Task<>() {
            @Override
            protected String call() {
                return releaseClient.fetchLatestVersionTag();
            }
        };

        fetchLatest.setOnSucceeded(e -> {
            String latest = fetchLatest.getValue();
            boolean offline = "Offline".equals(latest);
            options.latestVersion = latest;
            latestVersionValue.setText(latest);

            if (!offline && installedVersionCounts.containsKey(latest)) {
                setStatus("✓  Up to date", "status-ok");
            } else if (!offline && !installedVersionCounts.isEmpty()) {
                setStatus("↑  Update available", "status-update");
            } else {
                setStatus("", null);
            }
        });

        fetchLatest.setOnFailed(e -> {
            latestVersionValue.setText("Offline");
            setStatus("", null);
        });

        Thread thread = new Thread(fetchLatest, "genyo-version-check");
        thread.setDaemon(true);
        thread.start();
    }

    private void setStatus(String text, String styleClass) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-ok", "status-update");
        if (styleClass != null) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    private Map<String, Integer> scanInstalledVersions() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        PathSearcher searcher = new PathSearcher();
        String prismDir = searcher.searchPrism();
        String mcDir    = searcher.searchMC(true);

        for (String dir : List.of(prismDir, mcDir)) {
            if (dir.isEmpty()) continue;
            for (Path file : PathSearcher.findFilesStartingWithRecursive(dir, "genyo-addon-")) {
                String name = file.getFileName().toString();
                String[] parts = name.split("-");
                if (parts.length < 3) continue;
                String version = parts[2].replace(".jar", "");
                counts.merge(version, 1, Integer::sum);
            }
        }
        return counts;
    }

    private String formatInstalledVersions(Map<String, Integer> counts) {
        if (counts.isEmpty()) return "None";
        if (counts.size() > 2) return "Multiple";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (i++ > 0) sb.append(", ");
            sb.append(entry.getKey()).append(" (×").append(entry.getValue()).append(")");
        }
        return sb.toString();
    }
}