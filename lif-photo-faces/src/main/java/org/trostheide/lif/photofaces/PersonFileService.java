package org.trostheide.lif.photofaces;

import org.slf4j.Logger;
import org.trostheide.lif.core.LoggerService;

/**
 * Handles creation and update of person Markdown files.
 */
public class PersonFileService {
    private static final Logger log = LoggerService.getLogger(PersonFileService.class);
    public PersonFileService() {
        // Add config or dependencies as needed
    }

    /**
     * Creates or updates person markdown files for each cluster.
     */
    public void createOrUpdatePersonFiles() {
        log.info("Updating person markdown files.");
        // TODO: Create/update Markdown files, link to images
        log.info("Person markdown update complete.");
    }
}
