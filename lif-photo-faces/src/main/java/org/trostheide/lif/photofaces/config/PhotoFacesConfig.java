package org.trostheide.lif.photofaces.config;

public class PhotoFacesConfig {
    private final String imageDir;
    private final String personDir;
    private final boolean dryRun;
    private final String sinceDate;
    private final boolean debugMode;

    public PhotoFacesConfig(String imageDir, String personDir, boolean dryRun, String sinceDate, boolean debugMode) {
        this.imageDir = imageDir;
        this.personDir = personDir;
        this.dryRun = dryRun;
        this.sinceDate = sinceDate;
        this.debugMode=debugMode;
    }

    public String getImageDir() {
        return imageDir;
    }

    public String getPersonDir() {
        return personDir;
    }

    public boolean isDryRun() {
        return dryRun;
    }
    public boolean isDebugMode() {
        return debugMode;
    }

    public String getSinceDate() {
        return sinceDate;
    }

    @Override
    public String toString() {
        return "PhotoFacesConfig{" +
                "imageDir='" + imageDir + '\'' +
                ", personDir='" + personDir + '\'' +
                ", dryRun=" + dryRun +
                ", debugMode=" + debugMode +
                ", sinceDate='" + sinceDate + '\'' +
                '}';
    }


}
