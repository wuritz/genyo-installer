package dev.genyo.installer.api;

import dev.genyo.installer.InstallerOptions;
import dev.genyo.installer.LauncherType;
import dev.genyo.installer.net.GitHubReleaseClient;
import dev.genyo.installer.path.PathSearcher;
import dev.genyo.installer.ui.dialog.PrismInstanceSelectorDialog;
import dev.genyo.installer.ui.dialog.ProgressDialog;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstallerService {

    private final Stage ownerStage;
    private final InstallerOptions options;
    private final GitHubReleaseClient client;
    private final Runnable onReload;

    public InstallerService(Stage ownerStage, InstallerOptions options, Runnable onReload) {
        this.ownerStage = ownerStage;
        this.options = options;
        this.onReload = onReload;
        this.client = new GitHubReleaseClient(options.installerVersion);
    }

    /** Entry point, mirrors {@code InstallerScript.StartInstalling}. Call on the FX thread. */
    public void startInstalling() {
        options.installing = true;

        if (options.manualInstallLocation) {
            handleManualInstall();
            return;
        }

        PathSearcher pathSearcher = new PathSearcher();
        String dir;

        if (options.explicitLauncher && options.selectedExplicitLauncher == LauncherType.PRISM) {
            dir = pathSearcher.searchPrism();
        } else {
            dir = pathSearcher.searchMC();
        }

        if (dir.isEmpty()) {
            closeWithError("Couldn't find Minecraft nor PrismLauncher folder.");
            return;
        }

        if (pathSearcher.isUsingPrism()) {
            installPrism(dir);
        } else {
            installMC(dir);
        }
    }

    // ---------------------------------------------------------------
    // Manual install
    // ---------------------------------------------------------------

    private void handleManualInstall() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select the install folder");
        File selected = chooser.showDialog(ownerStage);

        if (selected == null) {
            closeWithError("Aborted.");
            return;
        }

        String dir = selected.getAbsolutePath();

        boolean proceed = confirm("You've entered '" + dir + "' as install location.\n\nDo you wish to proceed?");
        if (!proceed) {
            closeWithError("Aborted.");
            return;
        }

        if (PathSearcher.hasFilesStartingWith(dir, "genyo-addon-" + options.latestVersion)) {
            boolean stillProceed = confirm(
                    "You already have the latest Genyo version installed in this folder.\n\nDo you still want to proceed?");
            if (!stillProceed) {
                closeWithError("Aborted.");
                return;
            }
        }

        runDownloadThenInstall(downloadedJar -> {
            try {
                Path destination = Path.of(dir, downloadedJar.getFileName().toString());
                Files.copy(downloadedJar, destination, StandardCopyOption.REPLACE_EXISTING);
                cleanupTempFile(downloadedJar);
                finishSuccessfully();
            } catch (IOException e) {
                closeWithError("Couldn't copy the file into place: " + e.getMessage());
            }
        });
    }

    // ---------------------------------------------------------------
    // Prism Launcher install
    // ---------------------------------------------------------------

    private void installPrism(String prismDir) {
        Path instancesDir = Path.of(prismDir, "instances");
        if (!Files.isDirectory(instancesDir)) {
            closeWithError("Couldn't find 'instances' folder in Prism.");
            return;
        }

        List<String> instancesList = new ArrayList<>();
        File[] candidates = instancesDir.toFile().listFiles(File::isDirectory);
        if (candidates != null) {
            PathSearcher checker = new PathSearcher();
            for (File current : candidates) {
                Path modsDir = current.toPath().resolve("minecraft").resolve("mods");
                if (!Files.isDirectory(modsDir)) {
                    continue;
                }
                if (!checker.checkForFabricMeteor(modsDir.toString()) && !options.ignoreFabricMeteor) {
                    continue;
                }
                instancesList.add(current.getName());
            }
        }

        if (instancesList.isEmpty()) {
            closeWithError("You don't have any Prism instances with modding enabled. "
                    + "Create an instance where you enable Fabric modding and install Meteor Client first!");
            return;
        }

        PrismInstanceSelectorDialog selector = new PrismInstanceSelectorDialog(
                ownerStage, instancesList, !options.ignoreFabricMeteor);
        Optional<List<String>> result = selector.showAndWaitForSelection();

        if (result.isEmpty() || result.get().isEmpty()) {
            closeWithError("No instances were selected.");
            return;
        }

        List<String> selectedInstances = new ArrayList<>(result.get());

        // Check for duplicates / older versions, same as the original loop over a copy of the list.
        for (String instance : new ArrayList<>(selectedInstances)) {
            String modsPath = Path.of(instancesDir.toString(), instance, "minecraft", "mods").toString();
            if (PathSearcher.hasFilesStartingWith(modsPath, "genyo-addon-" + options.latestVersion)) {
                boolean stillProceed = confirm("You already have the latest Genyo version installed in the '"
                        + instance + "' instance.\n\nDo you still want to proceed?");
                if (!stillProceed) {
                    selectedInstances.remove(instance);
                }
            }
        }

        if (selectedInstances.isEmpty()) {
            closeWithError("No instances were selected.");
            return;
        }

        List<String> finalSelectedInstances = selectedInstances;
        runDownloadThenInstall(downloadedJar -> {
            try {
                for (String instance : finalSelectedInstances) {
                    Path instanceModsPath = Path.of(instancesDir.toString(), instance, "minecraft", "mods");
                    Path destination = instanceModsPath.resolve(downloadedJar.getFileName().toString());
                    Files.copy(downloadedJar, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                cleanupTempFile(downloadedJar);
                finishSuccessfully();
            } catch (IOException e) {
                closeWithError("Couldn't copy the file into place: " + e.getMessage());
            }
        });
    }

    // ---------------------------------------------------------------
    // Plain Minecraft Launcher install
    // ---------------------------------------------------------------

    private void installMC(String mcDir) {
        Path modsDir = Path.of(mcDir, "mods");
        if (!Files.isDirectory(modsDir)) {
            closeWithError("Couldn't find 'mods' folder.");
            return;
        }

        if (!new PathSearcher().checkForFabricMeteor(modsDir.toString()) && !options.ignoreFabricMeteor) {
            closeWithError("You don't have Fabric or Meteor installed in your 'mods' folder. Please install them first!");
            return;
        }

        if (PathSearcher.hasFilesStartingWith(modsDir.toString(), "genyo-addon-" + options.latestVersion)) {
            boolean stillProceed = confirm(
                    "You already have the latest Genyo version installed. (Minecraft Launcher) \n\nDo you still want to proceed?");
            if (!stillProceed) {
                closeWithError("Aborted.");
                return;
            }
        }

        runDownloadThenInstall(downloadedJar -> {
            try {
                Path destination = modsDir.resolve(downloadedJar.getFileName().toString());
                Files.copy(downloadedJar, destination, StandardCopyOption.REPLACE_EXISTING);
                cleanupTempFile(downloadedJar);
                finishSuccessfully();
            } catch (IOException e) {
                closeWithError("Couldn't copy the file into place: " + e.getMessage());
            }
        });
    }

    // ---------------------------------------------------------------
    // Shared download + progress dialog plumbing
    // ---------------------------------------------------------------

    private interface JarConsumer {
        void accept(Path downloadedJar);
    }

    private void runDownloadThenInstall(JarConsumer onDownloaded) {
        ProgressDialog progressDialog = new ProgressDialog(ownerStage);
        progressDialog.show();

        Task<Path> downloadTask = new Task<>() {
            @Override
            protected Path call() throws Exception {
                GitHubReleaseClient.ReleaseInfo release = client.fetchLatestRelease();
                return client.downloadJarToTemp(release, (percent, bytesRead, totalBytes) -> {
                    updateProgress(percent, 100);
                    updateMessage(formatBytes(bytesRead) + " / " + formatBytes(totalBytes));
                });
            }
        };

        progressDialog.bindTo(downloadTask);

        downloadTask.setOnSucceeded(e -> {
            progressDialog.close();
            onDownloaded.accept(downloadTask.getValue());
        });

        downloadTask.setOnFailed(e -> {
            progressDialog.close();
            Throwable ex = downloadTask.getException();
            closeWithError("Download failed: " + (ex != null ? ex.getMessage() : "unknown error"));
        });

        Thread thread = new Thread(downloadTask, "genyo-download");
        thread.setDaemon(true);
        thread.start();
    }

    private void cleanupTempFile(Path jarPath) {
        try {
            Files.deleteIfExists(jarPath);
            Files.deleteIfExists(jarPath.getParent());
        } catch (IOException ignored) {
            // best-effort cleanup, same spirit as the original's fire-and-forget File.Delete
        }
    }

    private void finishSuccessfully() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Genyo Addon installed successfully!", ButtonType.OK);
        alert.setTitle("Done");
        alert.setHeaderText(null);
        alert.initOwner(ownerStage);
        alert.showAndWait();

        options.installing = false;
        if (onReload != null) {
            onReload.run();
        }
    }

    private void closeWithError(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.initOwner(ownerStage);
            alert.showAndWait();
        });
        options.installing = false;
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation needed");
        alert.setHeaderText(null);
        alert.initOwner(ownerStage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }

}
