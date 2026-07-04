package dev.genyo.installer.ui.pane;

import dev.genyo.installer.util.InstallerService;
import dev.genyo.installer.util.ResourceReference;
import dev.genyo.installer.util.options.InstallerOptions;
import dev.genyo.installer.net.GitHubReleaseClient;
import dev.genyo.installer.util.path.PathSearcher;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HeroPane {

    private static final double WIDTH = 420;

    private final Stage ownerStage;
    private final InstallerOptions options;
    private final HostServices hostServices;
    private final GitHubReleaseClient releaseClient;

    private final Label installedVersionValue = new Label("—");
    private final Label latestVersionValue    = new Label("Fetching...");
    private final Label statusBadge           = new Label("");
    private final Button installButton        = new Button("Install Genyo");

    private final VBox root = new VBox();

    public HeroPane(Stage ownerStage, InstallerOptions options, HostServices hostServices) {
        this.ownerStage    = ownerStage;
        this.options       = options;
        this.hostServices  = hostServices;
        this.releaseClient = new GitHubReleaseClient(options.installerVersion);

        root.getStyleClass().add("hero-pane");
        root.setPrefWidth(WIDTH);
        root.setMinWidth(WIDTH);
        root.setMaxWidth(WIDTH);
        root.setPadding(new Insets(30, 32, 26, 32));

        installButton.getStyleClass().add("install-button");
        installButton.setMaxWidth(Double.MAX_VALUE);
        installButton.setOnAction(e -> onInstallClicked());
        VBox.setMargin(installButton, new Insets(0, 0, 14, 0));

        root.getChildren().addAll(
                buildHeader(),
                buildVersionBlock(),
                buildSpacer(),
                installButton,
                buildLinksRow());

        refreshLabels();
    }

    public Region getView() { return root; }

    // ---------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------

    private Region buildHeader() {
        ImageView logo = new ImageView(new Image(ResourceReference.IMG512_RES));
        logo.setFitWidth(50);
        logo.setFitHeight(50);
        logo.setPreserveRatio(true);

        Label title = new Label("Genyo Addon");
        title.getStyleClass().add("hero-title");

        HBox header = new HBox(14, logo, title);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(header, new Insets(0, 0, 30, 0));
        return header;
    }

    private Region buildVersionBlock() {
        Label installedLabel = new Label("INSTALLED");
        installedLabel.getStyleClass().add("hero-eyebrow");
        installedVersionValue.getStyleClass().add("hero-value");

        Label latestLabel = new Label("LATEST");
        latestLabel.getStyleClass().add("hero-eyebrow");
        latestVersionValue.getStyleClass().add("hero-value");
        statusBadge.getStyleClass().add("status-badge");
        statusBadge.setVisible(false);
        statusBadge.setManaged(false);

        HBox latestRow = new HBox(10, latestVersionValue, statusBadge);
        latestRow.setAlignment(Pos.CENTER_LEFT);

        Button changelogs = new Button("View changelogs →");
        changelogs.getStyleClass().add("changelog-button");
        changelogs.setOnAction(e -> openBrowser("https://genyo.dev/changelogs"));

        Region gap = new Region();
        gap.setPrefHeight(20);

        return new VBox(4, installedLabel, installedVersionValue, gap, latestLabel, latestRow, changelogs);
    }

    private Region buildSpacer() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Region buildLinksRow() {
        Button github  = textLink("GitHub",  "https://github.com/wuritz/genyo-addon");
        Button website = textLink("Website", "https://genyo.dev");
        Button discord = textLink("Discord", "https://genyo.dev/discord");

        Label dot1 = new Label("·");
        Label dot2 = new Label("·");
        dot1.getStyleClass().add("hero-link-dot");
        dot2.getStyleClass().add("hero-link-dot");

        HBox row = new HBox(6, github, dot1, website, dot2, discord);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Button textLink(String text, String url) {
        Button b = new Button(text);
        b.getStyleClass().add("hero-link-button");
        b.setOnAction(e -> openBrowser(url));
        return b;
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
                setStatus("Up to date", "status-badge-ok");
            } else if (!offline && !installedVersionCounts.isEmpty()) {
                setStatus("Update available", "status-badge-update");
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
        boolean visible = text != null && !text.isEmpty();
        statusBadge.setText(text == null ? "" : text);
        statusBadge.getStyleClass().removeAll("status-badge-ok", "status-badge-update");
        statusBadge.setVisible(visible);
        statusBadge.setManaged(visible);
        if (styleClass != null) {
            statusBadge.getStyleClass().add(styleClass);
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