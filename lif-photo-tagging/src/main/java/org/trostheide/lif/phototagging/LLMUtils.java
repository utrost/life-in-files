package org.trostheide.lif.phototagging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.trostheide.lif.phototagging.LLMResult;
import org.trostheide.lif.phototagging.PhotoTaggingConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;

public class LLMUtils {

    public static LLMResult callLLM(Path imagePath, PhotoTaggingConfig config) throws IOException, InterruptedException {
        // Step 1: Base64-encode the image
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

        // Step 2: Build JSON body
        String json = """
        {
            "model": "gemma3:4b",
            "messages": [{
                "role": "user",
                "content": "Describe the image",
                "images": [ "%s" ]
            }]
        }
        """.formatted(imageBase64);

        // Step 3: Send HTTP POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getOllamaEndpoint() + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        // Use InputStream to read the streaming response line-by-line
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
        ObjectMapper mapper = new ObjectMapper();

        String line;
        StringBuilder contentBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
          //  System.out.println("Ollama stream line: " + line); // Debug
            JsonNode jsonNode = mapper.readTree(line);
            String chunk = jsonNode.at("/message/content").asText();
            contentBuilder.append(chunk);
            if (jsonNode.has("done") && jsonNode.get("done").asBoolean()) {
                break;
            }
        }
        String lastContent = contentBuilder.toString();
        reader.close();

        // For this stub, tags and confidence are mocked. You can parse for tags as needed.
        return new LLMResult(lastContent.trim(), Collections.emptyList(), 1.0);
    }
}
