package org.trostheide.lif.phototagging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LLMUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static LLMResult queryLLM(Path image, PhotoTaggingConfig config) throws IOException, InterruptedException {
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(image));

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", config.getModel());
        payload.put("prompt", config.getPrompt());
        payload.put("images", List.of(base64));
        payload.put("stream", false);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.0);
        options.put("top_p", 0.9);
        options.put("top_k", 40);
        payload.put("options", options);

        // Debug output (without base64 image)
        Map<String, Object> debugPayload = new HashMap<>(payload);
        debugPayload.put("images", List.of("<omitted base64>"));
        System.out.println("Sending request to " + config.getApiEndpoint());
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(debugPayload));

        String jsonBody = MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getApiEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("LLM API call failed with HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = MAPPER.readTree(response.body());
        String responseText = root.has("response") ? root.get("response").asText() : "";

        System.out.println("raw response string:\n" + responseText);

        return LLMResult.fromJsonText(responseText);
    }
}
