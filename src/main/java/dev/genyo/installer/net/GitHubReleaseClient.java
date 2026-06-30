package dev.genyo.installer.net;

import dev.genyo.installer.json.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class GitHubReleaseClient {

    private static final String RELEASES_API_URL = "https://api.github.com/repos/wuritz/genyo-addon/releases";
    private final HttpClient httpClient;
    private final String userAgent;

    public GitHubReleaseClient(String installerVersion) {
        this.userAgent = "GenyoInstaller/" + installerVersion;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public record ReleaseInfo(String tagName, String jarFileName, String jarDownloadUrl) {}

    public interface ProgressListener {
        void onProgress(int percent, long bytesRead, long totalBytes);
    }

    public String fetchLatestVersionTag() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RELEASES_API_URL))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "Offline";
            }

            Map<String, Object> root = JsonParser.parseObject(response.body());
            Object tagName = root.get("tag_name");
            return tagName != null ? tagName.toString() : "Unknown";
        } catch (Exception e) {
            return "Offline";
        }
    }

    @SuppressWarnings("unchecked")
    public ReleaseInfo fetchLatestRelease() throws DownloadException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RELEASES_API_URL))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new DownloadException("GitHub API returned HTTP " + response.statusCode());
            }

            Map<String, Object> root = JsonParser.parseObject(response.body());
            String tagName = String.valueOf(root.get("tag_name"));

            Object assetsObj = root.get("assets");
            if (!(assetsObj instanceof List<?> assets)) {
                throw new DownloadException("No JAR file found in the latest GitHub release.");
            }

            for (Object assetObj : assets) {
                if (!(assetObj instanceof Map<?, ?> assetRaw)) {
                    continue;
                }
                Map<String, Object> asset = (Map<String, Object>) assetRaw;
                Object nameObj = asset.get("name");
                if (nameObj == null) {
                    continue;
                }
                String name = nameObj.toString();
                if (name.toLowerCase().endsWith(".jar")) {
                    String downloadUrl = String.valueOf(asset.get("browser_download_url"));
                    return new ReleaseInfo(tagName, name, downloadUrl);
                }
            }

            throw new DownloadException("No JAR file found in the latest GitHub release.");
        } catch (IOException | InterruptedException e) {
            throw new DownloadException("Network error while contacting GitHub: " + e.getMessage(), e);
        }
    }

    /**
     * Downloads a release's jar to a temp file, reporting progress along the way.
     *
     * @return the path to the downloaded temp file
     */
    public Path downloadJarToTemp(ReleaseInfo release, ProgressListener progress) throws DownloadException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(release.jarDownloadUrl()))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new DownloadException("Download failed with HTTP " + response.statusCode());
            }

            long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L);

            Path tempPath = Files.createTempFile("genyo-addon-", "-" + release.jarFileName());
            Path tempDir = Files.createTempDirectory("genyo-installer-");
            Path finalTempPath = tempDir.resolve(release.jarFileName());
            Files.deleteIfExists(tempPath);

            try (InputStream in = response.body();
                 OutputStream out = Files.newOutputStream(finalTempPath)) {
                byte[] buffer = new byte[8192];
                long bytesRead = 0;
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                    bytesRead += read;
                    if (progress != null) {
                        int percent = totalBytes > 0 ? (int) ((double) bytesRead / totalBytes * 100) : 0;
                        progress.onProgress(percent, bytesRead, totalBytes);
                    }
                }
            }

            return finalTempPath;
        } catch (IOException | InterruptedException e) {
            throw new DownloadException("Network error while downloading: " + e.getMessage(), e);
        }
    }

    public static class DownloadException extends Exception {
        public DownloadException(String message) {
            super(message);
        }

        public DownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
