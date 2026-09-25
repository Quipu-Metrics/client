package org.quipumetrics.core;

import org.junit.jupiter.api.Test;
import org.quipumetrics.core.chart.AdvancedBarChart;
import org.quipumetrics.core.chart.AdvancedPieChart;
import org.quipumetrics.core.chart.DrilldownPieChart;
import org.quipumetrics.core.chart.MultiLineChart;
import org.quipumetrics.core.chart.SimpleBarChart;
import org.quipumetrics.core.chart.SimplePieChart;
import org.quipumetrics.core.chart.SingleLineChart;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsPayloadTest {

    private Metrics metrics(File folder) {
        return Metrics.builder()
                .serviceId(1234)
                .serviceVersion("1.0.0")
                .platform("bukkit")
                .configFolder(folder)
                .playerCount(() -> 12)
                .platformVersion(() -> "git-Paper-196 (MC: 1.21.1)")
                .onlineMode(() -> 1)
                .taskRunner(Runnable::run)
                .logger(Logger.getLogger("test"))
                .build();
    }

    /** Writes a payload carrying every chart type for external schema validation. */
    @Test
    void writesPayloadForSchemaValidation() throws Exception {
        Path folder = Files.createTempDirectory("quipu");
        Metrics metrics = metrics(folder.toFile());
        metrics.start();

        metrics.addChart(new SingleLineChart("configured_worlds", () -> 4));
        metrics.addChart(new SimplePieChart("language_used", () -> "es"));

        Map<String, Integer> flat = new LinkedHashMap<>();
        flat.put("rewards", 812);
        flat.put("quests", 291);
        metrics.addChart(new AdvancedPieChart("enabled_modules", () -> flat));
        metrics.addChart(new SimpleBarChart("permission_plugin", () -> flat));
        metrics.addChart(new MultiLineChart("active_sessions", () -> flat));

        Map<String, Map<String, Integer>> nested = new LinkedHashMap<>();
        nested.put("sql", Collections.singletonMap("mariadb", 340));
        metrics.addChart(new DrilldownPieChart("storage_backend", () -> nested));

        metrics.addChart(new AdvancedBarChart("players_per_hour",
                () -> Collections.singletonMap("weekday", new int[]{4, 3, 2})));

        String payload = metrics.buildPayload();
        metrics.shutdown();

        Path out = Paths.get("build", "payload.json");
        Files.createDirectories(out.getParent());
        Files.write(out, payload.getBytes(StandardCharsets.UTF_8));

        assertTrue(payload.contains("\"playerAmount\":12"));
        assertTrue(payload.contains("\"platformVersion\":\"git-Paper-196 (MC: 1.21.1)\""));
    }

    /**
     * Every plugin on one host must report the same serverUUID. If they do not,
     * one host is counted as many and the data cannot be repaired afterwards.
     */
    @Test
    void allClientsOnOneHostShareTheServerUuid() throws Exception {
        Path folder = Files.createTempDirectory("quipu");
        MetricsConfig first = new MetricsConfig(folder.toFile());
        MetricsConfig second = new MetricsConfig(folder.toFile());

        assertEquals(first.getServerUuid(), second.getServerUuid());
        assertTrue(Files.exists(folder.resolve("config.properties")));
    }

    /** A chart whose value is unavailable is omitted, never sent as a placeholder. */
    @Test
    void unavailableChartsAreOmitted() throws Exception {
        Path folder = Files.createTempDirectory("quipu");
        Metrics metrics = metrics(folder.toFile());
        metrics.start();
        metrics.addChart(new SimplePieChart("language_used", () -> null));
        metrics.addChart(new SingleLineChart("boom", () -> {
            throw new IllegalStateException("plugin threw");
        }));

        String payload = metrics.buildPayload();
        metrics.shutdown();

        assertTrue(payload.contains("\"charts\":[]"));
        assertFalse(payload.contains("language_used"));
    }

    @Test
    void rejectsChartIdsTheBackendWouldDiscard() {
        assertThrows(IllegalArgumentException.class,
                () -> new SingleLineChart("Bad_Id", () -> 1));
    }

    @Test
    void escapesControlCharactersAndQuotes() {
        assertEquals("\"a\\\"b\\\\c\\nd\"", JsonObject.escape("a\"b\\c\nd"));
    }
}
