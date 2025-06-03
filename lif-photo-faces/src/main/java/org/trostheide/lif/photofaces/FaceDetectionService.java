package org.trostheide.lif.photofaces;

import org.trostheide.lif.photofaces.config.PhotoFacesConfig;
import org.trostheide.lif.core.LoggerService;

import org.slf4j.Logger;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

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
                ObjectNode faceNode = mapper.createObjectNode();
                if (config.isDebugMode()) {
                String faceCropName = imageFile.getName().replace(".jpg", "") + "_face" + faceIdx + ".png";
                String faceCropPath = new File(imageFile.getParentFile(), faceCropName).getAbsolutePath();
                Imgcodecs.imwrite(faceCropPath, faceRegion);
                    faceNode.put("face_crop", faceCropPath);
                }

                faceNode.put("face_index", faceIdx);
                faceNode.put("x", rect.x);
                faceNode.put("y", rect.y);
                faceNode.put("width", rect.width);
                faceNode.put("height", rect.height);


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
