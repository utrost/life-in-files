
// lif-core/src/test/java/org/trostheide/lif/core/CoreSmokeTest.java
package org.trostheide.lif.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CoreSmokeTest {

    @Test
    public void testConfigLoaderLoadsJson() throws Exception {
        File temp = Files.createTempFile("config", ".json").toFile();
        String content = "{ \"key\": \"value\" }";
        Files.writeString(temp.toPath(), content);
        ConfigLoader loader = new ConfigLoader();
        JsonNode node = loader.loadConfig(temp);
        assertEquals("value", node.get("key").asText());
    }

    @Test
    public void testLoggerService() {
        LoggerService svc = new LoggerService(CoreSmokeTest.class);
        svc.info("Info message");
        svc.error("Error message", new RuntimeException("err"));
    }

    @Test
    public void testProgressTracker() {
        ProgressTracker tracker = new ProgressTracker(100);
        tracker.step(50);
        tracker.complete();
    }

    @Test
    public void testLifIndexManager() throws Exception {
        File temp = Files.createTempFile("index", ".json").toFile();
        LifIndexManager mgr = new LifIndexManager(temp);
        List<JsonNode> empty = mgr.readIndex();
        assertTrue(empty.isEmpty());
        JsonNodeFactory f = JsonNodeFactory.instance;
        List<JsonNode> entries = List.of(f.objectNode().put("a", 1));
        mgr.writeIndex(entries);
        List<JsonNode> read = mgr.readIndex();
        assertEquals(1, read.size());
        assertEquals(1, read.get(0).get("a").asInt());
    }

    @Test
    public void testRetryExecutor() throws Exception {
        RetryExecutor exec = new RetryExecutor(3, 10);
        int result = exec.execute(() -> 42);
        assertEquals(42, result);
    }
}
