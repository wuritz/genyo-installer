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

    private static final String RELEASES_API_URL =
            "https://api.github.com/repos/wuritz/genyo-addon/releases/latest";

    private final HttpClient apiClient;
    private final HttpClient downloadClient;

    private final String userAgent;

    public GitHubReleaseClient(String installerVersion) {
        this.userAgent = "GenyoInstaller/" + installerVersion;

        this.apiClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.downloadClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public record ReleaseInfo(String tagName, String jarFileName, String jarDownloadUrl) {}

    public interface ProgressListener {
        void onProgress(int percent, long bytesRead, long totalBytes);
    }

    public String fetchLatestVersionTag() {
        try {
            String body = getApiResponseBody();
            Map<String, Object> root = JsonParser.parseObject(body);
            Object tagName = root.get("tag_name");
            return tagName != null ? tagName.toString() : "Unknown";
        } catch (Exception e) {
            return "Offline";
        }
    }

    @SuppressWarnings("unchecked")
    public ReleaseInfo fetchLatestRelease() throws DownloadException {
        try {
            String body = getApiResponseBody();
            Map<String, Object> root;
            try {
                root = JsonParser.parseObject(body);
            } catch (JsonParser.JsonParseException e) {
                String snippet = body.length() > 120 ? body.substring(0, 120) + "..." : body;
                throw new DownloadException(
                        "GitHub returned an unexpected response (not a JSON object).\n\nResponse started with:\n" + snippet);
            }

            String tagName = String.valueOf(root.get("tag_name"));

            Object assetsObj = root.get("assets");
            if (!(assetsObj instanceof List<?> assets)) {
                throw new DownloadException("No assets list found in the GitHub release response.");
            }

            for (Object assetObj : assets) {
                if (!(assetObj instanceof Map<?, ?> assetRaw)) continue;
                Map<String, Object> asset = (Map<String, Object>) assetRaw;
                Object nameObj = asset.get("name");
                if (nameObj == null) continue;
                String name = nameObj.toString();
                if (name.toLowerCase().endsWith(".jar")) {
                    String downloadUrl = String.valueOf(asset.get("browser_download_url"));
                    return new ReleaseInfo(tagName, name, downloadUrl);
                }
            }

            throw new DownloadException("No JAR file found in the latest GitHub release.");

        } catch (DownloadException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new DownloadException("Network error while contacting GitHub: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DownloadException("Unexpected error contacting GitHub: " + e.getMessage(), e);
        }
    }

    public Path downloadJarToTemp(ReleaseInfo release, ProgressListener progress) throws DownloadException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(release.jarDownloadUrl()))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    downloadClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() / 100 != 2) {
                throw new DownloadException("Download failed with HTTP " + response.statusCode());
            }

            long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L);

            Path tempDir = Files.createTempDirectory("genyo-installer-");
            Path finalTempPath = tempDir.resolve(release.jarFileName());

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
        } catch (DownloadException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new DownloadException("Network error while downloading: " + e.getMessage(), e);
        }
    }

    private String getApiResponseBody() throws IOException, InterruptedException, DownloadException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RELEASES_API_URL))
                .header("User-Agent", userAgent)
                .header("Accept", "application/vnd.github+json")
                //.header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = apiClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new DownloadException("GitHub API returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    public static class DownloadException extends Exception {
        public DownloadException(String message) { super(message); }
        public DownloadException(String message, Throwable cause) { super(message, cause); }
    }
}