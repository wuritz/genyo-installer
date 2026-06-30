package dev.genyo.installer.ui.tabs;

import dev.genyo.installer.InstallerOptions;
import dev.genyo.installer.api.InstallerService;
import dev.genyo.installer.net.GitHubReleaseClient;
import dev.genyo.installer.path.PathSearcher;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InstallerTab {

    private final Stage                 ownerStage;
    private final InstallerOptions      options;
    private final HostServices          hostServices;
    private final GitHubReleaseClient   releaseClient;

    private final Label  installedVersionValue  = new Label("...");
    private final Label  latestVersionValue     = new Label("Fetching...");
    private final Label  statusLabel            = new Label("");
    private final Button installButton          = new Button("Install Genyo");

    private final BorderPane root               = new BorderPane();

    private boolean offline = false;

    public InstallerTab(Stage ownerStage, InstallerOptions options, HostServices hostServices) {
        this.ownerStage = ownerStage;
        this.options = options;
        this.hostServices = hostServices;
        this.releaseClient = new GitHubReleaseClient(options.installerVersion);

        root.getStyleClass().add("installer-tab");
        root.setPadding(new Insets(18));

        root.setTop(buildHeader());
        root.setCenter(buildVersionInfo());
        root.setRight(buildLinkButtons());
        root.setBottom(buildInstallRow());

        refreshLabels();
    }

    public Region getView() {
        return root;
    }

    // ----------
    // Layout
    // ----------
    private Region buildHeader() {
        ImageView logo = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/genyo512.png"))));
        logo.setFitWidth(72);
        logo.setFitHeight(72);
        logo.setPreserveRatio(true);

        Label title = new Label("Genyo Addon");
        title.getStyleClass().add("app-title");

        HBox header = new HBox(16, logo, title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private Region buildVersionInfo() {
        Label installedLabel = new Label("Installed version:");
        installedLabel.getStyleClass().add("field-label");
        installedVersionValue.getStyleClass().add("field-value");

        Label latestLabel = new Label("Latest version:");
        latestLabel.getStyleClass().add("field-label");
        latestVersionValue.getStyleClass().add("field-value");

        Button changelogs = new Button("Changelogs");
        changelogs.setOnAction(e -> openBrowser("https://genyo.dev/changelogs"));

        VBox box = new VBox(4, installedLabel, installedVersionValue, new Region(), latestLabel, latestVersionValue, changelogs);
        box.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(changelogs, new Insets(14, 0, 0, 0));
        return box;
    }

    private Region buildLinkButtons() {
        Button github = new Button("GitHub");
        github.setOnAction(e -> openBrowser("https://genyo.dev/github"));

        Button website = new Button("Website");
        website.setOnAction(e -> openBrowser("https://genyo.dev"));

        Button discord = new Button("Discord");
        discord.setOnAction(e -> openBrowser("https://genyo.dev/discord"));

        for (Button b : List.of(github, website, discord)) {
            b.setPrefWidth(90);
        }

        VBox box = new VBox(8, github, website, discord);
        box.setAlignment(Pos.TOP_RIGHT);
        return box;
    }

    private Region buildInstallRow() {
        statusLabel.getStyleClass().add("status-label");

        installButton.getStyleClass().add("install-button");
        installButton.setPrefSize(190, 48);
        installButton.setOnAction(e -> onInstallClicked());

        VBox right = new VBox(6, installButton, statusLabel);
        right.setAlignment(Pos.CENTER_RIGHT);

        BorderPane row = new BorderPane();
        row.setRight(right);
        BorderPane.setMargin(right, new Insets(18, 0, 0, 0));
        return row;
    }

    // -----------
    // Behaviour
    // -----------
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

    public void refreshLabels() {
        Map<String, Integer> installedVersionCounts = scanInstalledVersions();
        installedVersionValue.setText(formatInstalledVersions(installedVersionCounts));

        latestVersionValue.setText("Fetching...");
        statusLabel.setText("");

        Task<String> fetchLatest = new Task<>() {
            @Override
            protected String call() {
                return releaseClient.fetchLatestVersionTag();
            }
        };

        fetchLatest.setOnSucceeded(e -> {
            String latest = fetchLatest.getValue();
            offline = "Offline".equals(latest);
            options.latestVersion = latest;
            latestVersionValue.setText(latest);

            if (installedVersionCounts.containsKey(latest)) {
                statusLabel.setText("Genyo is up to date!");
            } else if (!installedVersionCounts.isEmpty() && !offline) {
                statusLabel.setText("New Genyo is available!");
            } else {
                statusLabel.setText("");
            }
        });

        fetchLatest.setOnFailed(e -> {
            offline = true;
            latestVersionValue.setText("Offline");
            statusLabel.setText("");
        });

        Thread thread = new Thread(fetchLatest, "genyo-version-check");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Integer> scanInstalledVersions() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        PathSearcher searcher = new PathSearcher();

        String prismDir = searcher.searchPrism();
        String mcDir = searcher.searchMC(true);

        for (String dir : List.of(prismDir, mcDir)) {
            if (dir.isEmpty()) {
                continue;
            }
            for (Path file : PathSearcher.findFilesStartingWithRecursive(dir, "genyo-addon-")) {
                String name = file.getFileName().toString();
                String[] parts = name.split("-");
                if (parts.length < 3) {
                    continue;
                }
                String version = parts[2].replace(".jar", "");
                counts.merge(version, 1, Integer::sum);
            }
        }
        return counts;
    }

    private String formatInstalledVersions(Map<String, Integer> counts) {
        if (counts.isEmpty()) {
            return "None";
        }
        if (counts.size() > 2) {
            return "Multiple found.";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(" (").append(entry.getValue()).append(")");
            i++;
        }
        return sb.toString();
    }
}
