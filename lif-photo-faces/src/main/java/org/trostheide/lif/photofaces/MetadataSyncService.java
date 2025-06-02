package org.trostheide.lif.photofaces;

import org.trostheide.lif.photofaces.config.PhotoFacesConfig;

/**
 * Handles synchronization of person assignments to image sidecars and JPEG metadata.
 */
public class MetadataSyncService {
    private final PhotoFacesConfig config;

    public MetadataSyncService(PhotoFacesConfig config) {
        this.config = config;
    }

    /**
     * Main method for running sync between person files and image metadata.
     */
    public void runSync() {
        LoggerService.info("Starting metadata synchronization.");
        // TODO: Read renamed person files, update YAML and JPEG metadata accordingly
        LoggerService.info("Metadata synchronization completed.");
    }
}
