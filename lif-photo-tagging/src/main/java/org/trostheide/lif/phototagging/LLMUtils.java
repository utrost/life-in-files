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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends a base64-encoded image and prompt to the Ollama /api/generate endpoint.
 * Expected LLM: Gemma3 (vision-capable) via Ollama.
 */
public class LLMUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static LLMResult queryLLM(Path image, PhotoTaggingConfig config) throws IOException, InterruptedException {
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(image));

        // Structured prompt message
        List<Map<String, Object>> messages = List.of(Map.of(
                "role", "user",
                "content", config.getPrompt()
        ));

        // JSON schema definition
        Map<String, Object> format = new HashMap<>();
        format.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        properties.put("description", Map.of("type", "string"));
        properties.put("tags", Map.of("type", "array", "items", Map.of("type", "string")));
        format.put("properties", properties);
        format.put("required", List.of("description", "tags"));

        // Final payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", config.getModel());
        payload.put("messages", messages);
        payload.put("images", List.of(base64));
        payload.put("stream", false);
        payload.put("format", format);

        // Redacted printout
        Map<String, Object> debugPayload = new HashMap<>(payload);
        debugPayload.put("images", List.of("<omitted base64 image>"));

// Actual payload
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
        return LLMResult.fromJson(root);
    }


}
