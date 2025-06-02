package org.trostheide.lif.photofaces;

import org.trostheide.lif.photofaces.config.PhotoFacesConfig;

/**
 * Handles face detection and embedding extraction from images.
 */
public class FaceDetectionService {
    private final PhotoFacesConfig config;

    public FaceDetectionService(PhotoFacesConfig config) {
        this.config = config;
    }

    /**
     * Main method for running face detection over the input image directory.
     */
    public void runDetection() {
        LoggerService.info("Starting face detection in: " + config.getImageDir());
        // TODO: Iterate over images, detect faces, extract embeddings
        // For now, stub only.
        LoggerService.info("Face detection completed.");
    }
}
