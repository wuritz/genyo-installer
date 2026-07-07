package dev.genyo.installer.util.options;

public class InstallerOptions {

    public boolean manualInstallLocation = false;
    public boolean ignoreFabricMeteor = false;

    public volatile boolean installing = false;
    public volatile String latestVersion = "";

    public final String installerVersion;

    public InstallerOptions(String installerVersion) {
        this.installerVersion = installerVersion;
    }

}
