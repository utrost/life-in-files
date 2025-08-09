package org.trostheide.lif.photofaces;

import org.slf4j.Logger;
import org.trostheide.lif.core.LoggerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import smile.clustering.DBSCAN;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles clustering of face embeddings to form proto-person groups.
 */
public class FaceClusteringService {
    private static final Logger log = LoggerService.getLogger(FaceClusteringService.class);

    public FaceClusteringService() {}

    /**
     * Normalize an embedding vector to unit length.
     */
    private static double[] normalize(double[] v) {
        double norm = 0;
        for (double x : v) norm += x * x;
        norm = Math.sqrt(norm);
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] / (norm + 1e-10);
        return out;
    }

    /**
     * Main method to cluster face embeddings and return cluster assignments.
     */
    public void runClustering(String imageDir) {
        log.info("Starting face clustering.");

        File jsonFile = Paths.get(imageDir, "face-detections.json").toFile();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode facesFlat = mapper.createArrayNode();
        List<double[]> embeddingList = new ArrayList<>();

        // Step 1: Load embeddings and flatten all faces across all images
        try {
            JsonNode root = mapper.readTree(jsonFile);
            int imageIdx = 0;
            for (JsonNode imageNode : root) {
                String imagePath = imageNode.get("image").asText();
                ArrayNode faces = (ArrayNode) imageNode.get("faces");
                int faceIdx = 0;
                for (JsonNode faceNode : faces) {
                    ArrayNode embeddingNode = (ArrayNode) faceNode.get("embedding");
                    double[] embedding = new double[embeddingNode.size()];
                    for (int i = 0; i < embedding.length; i++) {
                        embedding[i] = embeddingNode.get(i).asDouble();
                    }

                    // Normalize embedding
                    double[] normEmbedding = normalize(embedding);
                    embeddingList.add(normEmbedding);

                    ObjectNode flat = mapper.createObjectNode();
                    flat.put("image", imagePath);
                    flat.put("face_index", faceIdx);
                    flat.put("x", faceNode.get("x").asInt());
                    flat.put("y", faceNode.get("y").asInt());
                    flat.put("width", faceNode.get("width").asInt());
                    flat.put("height", faceNode.get("height").asInt());
                    if (faceNode.has("face_crop")) {
                        flat.put("face_crop", faceNode.get("face_crop").asText());
                    }
                    flat.set("embedding", embeddingNode);
                    facesFlat.add(flat);

                    faceIdx++;
                }
                imageIdx++;
            }
        } catch (Exception e) {
            log.error("Failed to load or parse face-detections.json", e);
            return;
        }

        if (embeddingList.isEmpty()) {
            log.error("No embeddings found for clustering.");
            return;
        }

        // Step 2: Run DBSCAN clustering on normalized embeddings
        double[][] X = embeddingList.toArray(new double[0][]);

        int minPts = 3;     // Minimum cluster size, tune as needed
        double eps = 0.5;   // Typical for normalized face embeddings; tune as needed

        log.info("Running DBSCAN with eps=" + eps + " and minPts=" + minPts + " on " + X.length + " embeddings.");

        DBSCAN<double[]> dbscan = DBSCAN.fit(X, minPts, eps);
        int[] labels = dbscan.y;

        // Print a sample of cluster labels for inspection
        StringBuilder labelSample = new StringBuilder();
        for (int i = 0; i < Math.min(labels.length, 20); i++) {
            labelSample.append(labels[i]).append(" ");
        }
        log.info("Sample of first 20 cluster labels: " + labelSample);

        // Step 3: Write output with cluster labels
        ArrayNode outArray = mapper.createArrayNode();
        for (int i = 0; i < facesFlat.size(); i++) {
            ObjectNode faceNode = (ObjectNode) facesFlat.get(i);
            faceNode.put("cluster", labels[i]);
            outArray.add(faceNode);
        }

        File outFile = Paths.get(imageDir, "face-clusters.json").toFile();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, outArray);
            log.info("Clustering results written to: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to write clustering results.", e);
        }

        // Robust cluster counting (excluding noise, which is -1)
        Set<Integer> clusters = new HashSet<>();
        for (int c : labels) {
            if (c >= 0) clusters.add(c);
        }
        log.info("Face clustering completed. Total faces: " + facesFlat.size() + ", clusters: " + clusters.size());
    }
}
