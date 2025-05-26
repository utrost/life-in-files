package org.trostheide.lif.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages reading and writing of the .lif-index.json file.
 */
public class LifIndexManager {
    private final File indexFile;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param indexFile the .lif-index.json file to read/write
     */
    public LifIndexManager(File indexFile) {
        this.indexFile = indexFile;
    }

    /**
     * Reads the current index, returning an empty list if the file doesn't exist yet.
     */
    public synchronized List<JsonNode> readIndex() throws IOException {
        if (!indexFile.exists()) {
            return new ArrayList<>();
        }
        return mapper.readValue(indexFile, new TypeReference<List<JsonNode>>() {});
    }

    /**
     * Writes the full list of entries back to the index file.
     */
    public synchronized void writeIndex(List<JsonNode> entries) throws IOException {
        // ensure parent dirs exist
        File parent = indexFile.getParentFile();
        if (parent != null) parent.mkdirs();
        mapper.writerWithDefaultPrettyPrinter().writeValue(indexFile, entries);
    }

    /**
     * Appends a single entry (source→output with timestamp) to the index.
     */
    public synchronized void writeIndexEntry(File source, File output) throws IOException {
        List<JsonNode> entries = readIndex();
        ObjectNode entry = mapper.createObjectNode()
                .put("source", source.getAbsolutePath())
                .put("output", output.getAbsolutePath())
                .put("timestamp", Instant.now().toString());
        entries.add(entry);
        writeIndex(entries);
    }
}
