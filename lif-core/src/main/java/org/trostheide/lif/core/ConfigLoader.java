// lif-core/src/main/java/org/trostheide/lif/core/ConfigLoader.java
package org.trostheide.lif.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.IOException;

public class ConfigLoader {
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public ConfigLoader() {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Loads a configuration file (JSON or YAML) into a JsonNode.
     */
    public JsonNode loadConfig(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            return yamlMapper.readTree(file);
        } else {
            return jsonMapper.readTree(file);
        }
    }

    /**
     * Validates the loaded JsonNode against a JSON schema. Stub implementation.
     */
    public void validate(JsonNode node, JsonNode schema) throws JsonProcessingException {
        // TODO: implement JSON Schema validation
    }
}

