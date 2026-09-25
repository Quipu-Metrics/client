package org.quipumetrics.core;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/**
 * The configuration file shared by every Quipu client on one host.
 * <p>
 * Sharing it is what keeps the {@code serverUuid} identical across plugins. A
 * client that wrote its own copy inside its own plugin folder would make one
 * host count as many, and that cannot be repaired afterwards.
 */
public final class MetricsConfig {

    private final File file;

    @Getter
    private String serverUuid;
    @Getter
    private boolean enabled = true;
    @Getter
    private boolean logErrors;
    @Getter
    private boolean logSentData;

    public MetricsConfig(File folder) {
        this.file = new File(folder, "config.properties");
        reload();
    }

    /**
     * Re-reads the file. Called before every submission so that an operator who
     * sets {@code enabled=false} is obeyed without restarting the server.
     */
    public void reload() {
        if (!file.exists()) create();

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file.toPath())) {
            properties.load(in);
        } catch (IOException e) {
            // An unreadable config must not take the server down, and must not
            // silently opt the host in either.
            enabled = false;
            return;
        }

        enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
        logErrors = Boolean.parseBoolean(properties.getProperty("logErrors", "false"));
        logSentData = Boolean.parseBoolean(properties.getProperty("logSentData", "false"));

        serverUuid = properties.getProperty("serverUuid");
        if (serverUuid == null) enabled = false;
    }

    private void create() {
        Properties properties = new Properties();
        properties.setProperty("enabled", "true");
        properties.setProperty("serverUuid", UUID.randomUUID().toString());
        properties.setProperty("logErrors", "false");
        properties.setProperty("logSentData", "false");

        File folder = file.getParentFile();
        if (folder != null) folder.mkdirs();

        try {
            File temp = File.createTempFile("quipu", ".tmp", folder);
            try (OutputStream out = Files.newOutputStream(temp.toPath())) {
                properties.store(out, "Quipu Metrics. Set enabled=false to opt this host out of all data collection.");
            }

            // Several plugins can start at once and race to create this file.
            // Moving without REPLACE_EXISTING means the loser fails here and
            // then reads the winner's file, so every plugin ends up with the
            // same serverUuid.
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException alreadyCreated) {
                temp.delete();
            }
        } catch (IOException ignored) {
            // reload() will find no file and opt out.
        }
    }
}
