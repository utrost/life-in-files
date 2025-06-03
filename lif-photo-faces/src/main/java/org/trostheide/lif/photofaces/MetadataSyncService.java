package org.trostheide.lif.photofaces;

import org.slf4j.Logger;
import org.trostheide.lif.photofaces.config.PhotoFacesConfig;
import org.trostheide.lif.core.LoggerService;

/**
 * Handles synchronization of person assignments to image sidecars and JPEG metadata.
 */
public class MetadataSyncService {
    private static final Logger log = LoggerService.getLogger(MetadataSyncService.class);
    private final PhotoFacesConfig config;

    public MetadataSyncService(PhotoFacesConfig config) {
        this.config = config;
    }

    /**
     * Main method for running sync between person files and image metadata.
     */
    public void runSync() {
        log.info("Starting metadata synchronization.");
        // TODO: Read renamed person files, update YAML and JPEG metadata accordingly
        log.info("Metadata synchronization completed.");
    }
}
