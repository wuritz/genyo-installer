package dev.genyo.installer;

public class InstallerOptions {

    public boolean manualInstallLocation = false;
    public boolean explicitLauncher = true;

    public LauncherType selectedExplicitLauncher = LauncherType.PRISM;

    public boolean ignoreFabricMeteor = false;

    public volatile boolean installing = false;
    public volatile String latestVersion = "";

    public final String installerVersion;

    public InstallerOptions(String installerVersion) {
        this.installerVersion = installerVersion;
    }

}
