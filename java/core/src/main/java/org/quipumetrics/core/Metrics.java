package org.quipumetrics.core;

import lombok.Builder;
import lombok.NonNull;
import org.quipumetrics.core.chart.CustomChart;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects a small set of facts about this host and sends them to Quipu Metrics
 * every 30 minutes.
 * <p>
 * Platform modules build one of these and hand it the four things only the
 * platform can answer. Everything else lives here, so an adapter stays thin.
 */
@Builder
public final class Metrics {

    private static final String DEFAULT_BASE_URL = "https://api.quipumetrics.org/v1/data/";
    private static final int MAX_CHARTS = 32;
    private static final long PERIOD_MINUTES = 30L;

    private final int serviceId;
    @NonNull
    private final String serviceVersion;
    @NonNull
    private final String platform;
    @NonNull
    private final File configFolder;
    @NonNull
    private final Callable<Integer> playerCount;
    @NonNull
    private final Callable<String> platformVersion;
    @NonNull
    private final Callable<Integer> onlineMode;
    /**
     * Runs the given task where reading platform state is safe. On platforms with
     * a main game thread that means the main thread; elsewhere it may run the
     * task directly.
     */
    @NonNull
    private final Consumer<Runnable> taskRunner;
    @NonNull
    private final Logger logger;
    @Builder.Default
    private final String baseUrl = DEFAULT_BASE_URL;

    private final List<CustomChart> charts = new CopyOnWriteArrayList<>();

    private MetricsConfig config;
    private ScheduledExecutorService scheduler;

    public void addChart(CustomChart chart) {
        if (charts.size() < MAX_CHARTS) charts.add(chart);
    }

    public void start() {
        config = new MetricsConfig(configFolder);
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "quipu-metrics");
            thread.setDaemon(true);
            return thread;
        });

        // A random first delay spreads load. Aligning to the clock would put every
        // server on the planet into two spikes per hour.
        long initialDelay = TimeUnit.MINUTES.toSeconds(3)
                + ThreadLocalRandom.current().nextInt((int) TimeUnit.MINUTES.toSeconds(3));
        scheduler.scheduleAtFixedRate(this::tick, initialDelay,
                TimeUnit.MINUTES.toSeconds(PERIOD_MINUTES), TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void tick() {
        // Re-read every cycle so that disabling collection takes effect without
        // a restart.
        config.reload();
        if (!config.isEnabled()) return;

        taskRunner.accept(() -> {
            String payload = buildPayload();
            scheduler.execute(() -> submit(payload));
        });
    }

    private void submit(String payload) {
        if (config.isLogSentData())
            logger.info("Quipu Metrics sending: " + payload);

        try {
            new Submitter(baseUrl).send(platform, payload);
        } catch (Exception e) {
            if (config.isLogErrors()) {
                logger.log(Level.WARNING, "Quipu Metrics submission failed", e);
            }
        }
    }

    String buildPayload() {
        JsonArray chartData = new JsonArray();
        for (CustomChart chart : charts) {
            JsonObject json = chart.toJson();
            if (json != null) chartData.add(json);
        }

        JsonObject service = new JsonObject()
                .add("id", serviceId)
                .add("version", serviceVersion)
                .add("charts", chartData);

        return new JsonObject()
                .add("serverUUID", config.getServerUuid())
                .add("playerAmount", call(playerCount, -1))
                .add("onlineMode", call(onlineMode, -1))
                .add("platformVersion", call(platformVersion, "unknown"))
                .add("javaVersion", System.getProperty("java.version"))
                .add("osName", System.getProperty("os.name"))
                .add("osArch", System.getProperty("os.arch"))
                .add("osVersion", System.getProperty("os.version"))
                .add("coreCount", Runtime.getRuntime().availableProcessors())
                .add("service", service)
                .toString();
    }

    private <T> T call(Callable<T> supplier, T fallback) {
        try {
            T value = supplier.call();
            return value == null ? fallback : value;
        } catch (Exception e) {
            return fallback;
        }
    }
}
