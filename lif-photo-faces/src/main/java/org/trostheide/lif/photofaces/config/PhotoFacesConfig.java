package org.trostheide.lif.photofaces.config;

public class PhotoFacesConfig {
    private final String imageDir;
    private final String personDir;
    private final boolean dryRun;
    private final String sinceDate;

    public PhotoFacesConfig(String imageDir, String personDir, boolean dryRun, String sinceDate) {
        this.imageDir = imageDir;
        this.personDir = personDir;
        this.dryRun = dryRun;
        this.sinceDate = sinceDate;
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

    public String getSinceDate() {
        return sinceDate;
    }

    @Override
    public String toString() {
        return "PhotoFacesConfig{" +
                "imageDir='" + imageDir + '\'' +
                ", personDir='" + personDir + '\'' +
                ", dryRun=" + dryRun +
                ", sinceDate='" + sinceDate + '\'' +
                '}';
    }

    // Optional: with-methods for "modifying" config in a functional style (creates new instance)
    public PhotoFacesConfig withImageDir(String imageDir) {
        return new PhotoFacesConfig(imageDir, this.personDir, this.dryRun, this.sinceDate);
    }

    public PhotoFacesConfig withPersonDir(String personDir) {
        return new PhotoFacesConfig(this.imageDir, personDir, this.dryRun, this.sinceDate);
    }

    public PhotoFacesConfig withDryRun(boolean dryRun) {
        return new PhotoFacesConfig(this.imageDir, this.personDir, dryRun, this.sinceDate);
    }

    public PhotoFacesConfig withSinceDate(String sinceDate) {
        return new PhotoFacesConfig(this.imageDir, this.personDir, this.dryRun, sinceDate);
    }
}
