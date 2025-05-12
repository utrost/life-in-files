
// lif-core/src/main/java/org/trostheide/lif/core/LifIndexManager.java
package org.trostheide.lif.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LifIndexManager {
    private final File indexFile;
    private final ObjectMapper mapper;

    public LifIndexManager(File indexFile) {
        this.indexFile = indexFile;
        this.mapper = new ObjectMapper();
    }

    public List<JsonNode> readIndex() throws IOException {
        if (!indexFile.exists()) {
            return new ArrayList<>();
        }
        JsonNode root = mapper.readTree(indexFile);
        if (root.isArray()) {
            List<JsonNode> entries = new ArrayList<>();
            root.forEach(entries::add);
            return entries;
        }
        return new ArrayList<>();
    }

    public void writeIndex(List<JsonNode> entries) throws IOException {
        ArrayNode array = mapper.createArrayNode();
        entries.forEach(array::add);
        Files.createDirectories(indexFile.toPath().getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(indexFile, array);
    }
}

