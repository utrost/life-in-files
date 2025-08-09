package org.trostheide.lif.photofaces;

import org.opencv.imgproc.Imgproc;
import org.trostheide.lif.photofaces.config.PhotoFacesConfig;
import org.trostheide.lif.core.LoggerService;

import org.slf4j.Logger;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles face detection and embedding extraction from images.
 */
public class FaceDetectionService {
    private static final Logger log = LoggerService.getLogger(FaceDetectionService.class);
    private final PhotoFacesConfig config;
    private Net faceEmbedder;

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            log.info("OpenCV library loaded successfully.");
        } catch (Exception e) {
            System.err.println("OpenCV not loaded: " + e.getMessage());
        }
    }

    public FaceDetectionService(PhotoFacesConfig config) {
        this.config = config;
        try {
            String embedderPath = getEmbedderModelPath();
            this.faceEmbedder = Dnn.readNetFromTorch(embedderPath);
            log.info("Loaded face embedder model from: " + embedderPath);
        } catch (IOException e) {
            log.error("Could not load face embedder model.", e);
            this.faceEmbedder = null;
        }
    }

    /**
     * Loads the Haar Cascade file from resources and returns a path to a temporary file.
     */
    private static String getCascadeFilePath() throws IOException {
        InputStream in = FaceDetectionService.class.getResourceAsStream("/haarcascade_frontalface_default.xml");
        if (in == null) {
            throw new FileNotFoundException("Resource haarcascade_frontalface_default.xml not found");
        }
        Path temp = Files.createTempFile("haarcascade_frontalface_default", ".xml");
        Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        temp.toFile().deleteOnExit();
        return temp.toAbsolutePath().toString();
    }

    /**
     * Loads the face embedding model file from resources and returns a path to a temporary file.
     */
    private static String getEmbedderModelPath() throws IOException {
        InputStream in = FaceDetectionService.class.getResourceAsStream("/nn4.small2.v1.t7");
        if (in == null) {
            throw new FileNotFoundException("Resource nn4.small2.v1.t7 not found");
        }
        Path temp = Files.createTempFile("nn4.small2.v1", ".t7");
        Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        temp.toFile().deleteOnExit();
        return temp.toAbsolutePath().toString();
    }

    /**
     * Extracts the embedding vector from a detected face region (expects normalized 96x96 face).
     */
    private float[] extractEmbedding(Mat faceRegionNormalized) {
        // Prepare input blob
        Mat inputBlob = Dnn.blobFromImage(faceRegionNormalized, 1.0/255, new Size(96, 96), new Scalar(0,0,0), false, false);

        // Forward pass
        faceEmbedder.setInput(inputBlob);
        Mat output = faceEmbedder.forward();

        float[] embedding = new float[(int)output.total()];
        output.get(0, 0, embedding);

        return embedding;
    }

    /**
     * Main method for running face detection over the input image directory and its subdirectories.
     */
    public void runDetection() {
        log.info("Starting face detection in: " + config.getImageDir());

        File imgDir = new File(config.getImageDir());
        if (!imgDir.exists() || !imgDir.isDirectory()) {
            log.error("Image directory does not exist: " + imgDir.getAbsolutePath());
            return;
        }

        String cascadePath;
        try {
            cascadePath = getCascadeFilePath();
        } catch (IOException e) {
            log.error("Could not load Haar Cascade from resources", e);
            return;
        }

        CascadeClassifier faceDetector = new CascadeClassifier(cascadePath);
        if (faceDetector.empty()) {
            log.error("Failed to load Haar Cascade from " + cascadePath);
            return;
        }
        if (this.faceEmbedder == null) {
            log.error("Face embedder model was not loaded. Cannot extract embeddings.");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode detectionResults = mapper.createArrayNode();

        List<File> imageFiles = new ArrayList<>();
        try {
            Files.walk(imgDir.toPath())
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg");
                    })
                    .forEach(p -> imageFiles.add(p.toFile()));
        } catch (IOException e) {
            log.error("Error traversing image directory tree.", e);
            return;
        }

        if (imageFiles.isEmpty()) {
            log.error("No images found in directory or subdirectories.");
            return;
        }

        // Ensure the personDir exists
        String personDir = config.getPersonDir();
        File personDirFile = new File(personDir);
        if (!personDirFile.exists()) {
            if (!personDirFile.mkdirs()) {
                log.error("Failed to create person directory: " + personDir);
                return;
            }
        }

        int totalImages = 0;
        int totalWithFaces = 0;

        for (File imageFile : imageFiles) {
            totalImages++;
            log.info("Processing: " + imageFile.getAbsolutePath());

            Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
            if (image.empty()) {
                log.error("Could not read image: " + imageFile.getAbsolutePath());
                continue;
            }

            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(
                    image,
                    faces,
                    1.2,                // scaleFactor
                    7,                  // minNeighbors
                    0,                  // flags
                    new Size(300, 300),   // minSize
                    new Size()          // maxSize
            );
            Rect[] facesArray = faces.toArray();

            if (facesArray.length < 1 || facesArray.length > 3) {
                log.info("Skipping image (faces found: " + facesArray.length + "): " + imageFile.getAbsolutePath());
                continue;
            }

            totalWithFaces++;
            ObjectNode imageNode = mapper.createObjectNode();
            imageNode.put("image", imageFile.getAbsolutePath());
            ArrayNode facesNode = mapper.createArrayNode();

            int faceIdx = 0;
            for (Rect rect : facesArray) {
                Mat faceRegion = new Mat(image, rect);

                // Resize face crop to 96x96 for both embedding and (optionally) saving PNG
                Mat normalizedFace = new Mat();
                Imgproc.resize(faceRegion, normalizedFace, new Size(96, 96));

                ObjectNode faceNode = mapper.createObjectNode();

                if (config.isDebugMode()) {
                    String baseName = imageFile.getName().replaceAll("\\.jpe?g$", "");
                    String faceCropName = baseName + "_face" + faceIdx + ".png";
                    String faceCropPath = new File(personDirFile, faceCropName).getAbsolutePath();
                    Imgcodecs.imwrite(faceCropPath, normalizedFace);  // Save the normalized 96x96 crop
                    faceNode.put("face_crop", faceCropPath);
                }

                faceNode.put("face_index", faceIdx);
                faceNode.put("x", rect.x);
                faceNode.put("y", rect.y);
                faceNode.put("width", rect.width);
                faceNode.put("height", rect.height);

                // Extract and add embedding (from normalized crop)
                float[] embedding = extractEmbedding(normalizedFace);
                ArrayNode embeddingNode = mapper.createArrayNode();
                for (float v : embedding) embeddingNode.add(v);
                faceNode.set("embedding", embeddingNode);

                facesNode.add(faceNode);
                faceIdx++;
            }

            imageNode.set("faces", facesNode);
            detectionResults.add(imageNode);
            log.info("Faces found: " + facesArray.length);
        }

        File outJson = new File(config.getImageDir(), "face-detections.json");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(outJson, detectionResults);
            log.info("Face detection results written to: " + outJson.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write detection results: " + e.getMessage());
        }

        log.info("Processed images: " + totalImages + ", with faces: " + totalWithFaces);
        log.info("Face detection completed.");
    }
}
