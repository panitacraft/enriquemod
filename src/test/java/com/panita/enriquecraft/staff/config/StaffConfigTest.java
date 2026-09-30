package com.panita.enriquecraft.staff.config;

import com.panita.enriquecraft.core.framework.config.ConfigIssue;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.config.ConfigReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffConfigTest {

    @TempDir
    Path directory;

    private StaffConfig bindWith(String maxPerPlayer) throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"),
                "{ \"staff\": { \"deathRecords\": { \"maxPerPlayer\": " + maxPerPlayer + " } } }");
        return new ConfigManager(directory).bind("staff", StaffConfig.class);
    }

    @Test
    void defaultsToTwenty() {
        ConfigManager manager = new ConfigManager(directory);

        StaffConfig config = manager.bind("staff", StaffConfig.class);

        assertEquals(20, config.maxDeathRecordsPerPlayer.get());
        assertTrue(manager.reload().isClean());
    }

    @Test
    void acceptsTheWholeRange() throws IOException {
        assertEquals(1, bindWith("1").maxDeathRecordsPerPlayer.get());
        assertEquals(200, bindWith("200").maxDeathRecordsPerPlayer.get());
    }

    @Test
    void valuesOutsideTheRangeFallBackToTwentyWithAReport() throws IOException {
        for (String value : List.of("0", "201", "-5", "\"many\"", "2.5")) {
            Files.writeString(directory.resolve("enriquecraft.json5"),
                    "{ \"staff\": { \"deathRecords\": { \"maxPerPlayer\": " + value + " } } }");
            ConfigManager manager = new ConfigManager(directory);
            StaffConfig config = manager.bind("staff", StaffConfig.class);

            ConfigReport report = manager.reload();

            assertEquals(20, config.maxDeathRecordsPerPlayer.get(), value);
            assertEquals(ConfigIssue.Kind.INVALID_VALUE, report.issues().get(0).kind(), value);
            assertEquals("staff.deathRecords.maxPerPlayer", report.issues().get(0).path(), value);
        }
    }
}
