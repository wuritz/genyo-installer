package dev.genyo.installer.path;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class PathSearcher {

    private boolean usingPrism = false;

    private static Path appDataRoot() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", "");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData != null ? Path.of(appData) : Path.of(home, "AppData", "Roaming");
        } else if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support");
        } else {
            // Linux / other: most Minecraft launchers use ~/.minecraft or ~/.local/share
            return Path.of(home);
        }
    }

    /** Searches for a ".minecraft" folder, optionally falling back to Prism Launcher. */
    public String searchMC() {
        return searchMC(false);
    }

    public String searchMC(boolean noPrism) {
        Path dir = appDataRoot().resolve(".minecraft");

        if (!Files.isDirectory(dir)) {
            if (noPrism) {
                return "";
            }

            String prismDir = searchPrism();
            if (prismDir.isEmpty()) {
                return "";
            }

            usingPrism = true;
            return prismDir;
        }

        return dir.toString();
    }

    /** Searches for a "PrismLauncher" folder. */
    public String searchPrism() {
        Path dir = appDataRoot().resolve("PrismLauncher");

        if (!Files.isDirectory(dir)) {
            return "";
        }

        usingPrism = true;
        return dir.toString();
    }

    public boolean isUsingPrism() {
        return usingPrism;
    }

    /** Returns true if the given "mods" folder contains both Fabric API and Meteor Client jars. */
    public boolean checkForFabricMeteor(String modsPath) {
        boolean hasFabric = matchesAnyFile(modsPath, "^fabric-api-.*");
        boolean hasMeteor = matchesAnyFile(modsPath, "^meteor-client-.*");
        return hasFabric && hasMeteor;
    }

    private boolean matchesAnyFile(String dirPath, String regex) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        for (File f : files) {
            if (f.isFile() && pattern.matcher(f.getName()).find()) {
                return true;
            }
        }
        return false;
    }

    /** True if a directory contains any file whose name starts with the given prefix. */
    public static boolean hasFilesStartingWith(String dirPath, String prefix) {
        Path dir = Path.of(dirPath);
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, prefix + "*")) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Recursively lists files under a directory whose name starts with the given
     * prefix (mirrors {@code Directory.EnumerateFiles(dir, pattern, AllDirectories)}).
     */
    public static List<Path> findFilesStartingWithRecursive(String dirPath, String prefix) {
        java.util.List<Path> result = new java.util.ArrayList<>();
        Path dir = Path.of(dirPath);
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (var walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .forEach(result::add);
        } catch (IOException ignored) {
            // best-effort, same as the original which just enumerates what it can
        }
        return result;
    }

}
