package dev.genyo.installer.api.options;

public class InstallerOptions {

    public boolean manualInstallLocation = false;
    public boolean manualVersionSelect = false;
    public boolean ignoreFabricMeteor = false;

    public volatile boolean installing = false;
    public volatile String latestVersion = "";

    public final String installerVersion;

    public InstallerOptions(String installerVersion) {
        this.installerVersion = installerVersion;
    }

}
