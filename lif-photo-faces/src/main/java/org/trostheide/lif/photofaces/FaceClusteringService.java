package org.trostheide.lif.photofaces;

/**
 * Handles clustering of face embeddings to form proto-person groups.
 */
public class FaceClusteringService {

    public FaceClusteringService() {
        // Add config or dependencies as needed
    }

    /**
     * Main method to cluster face embeddings and return cluster assignments.
     */
    public void runClustering() {
        LoggerService.info("Starting face clustering.");
        // TODO: Load embeddings, run clustering algorithm (K-means/DBSCAN)
        LoggerService.info("Face clustering completed.");
    }
}
