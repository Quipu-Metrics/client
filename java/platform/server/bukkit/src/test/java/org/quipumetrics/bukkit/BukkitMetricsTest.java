package org.quipumetrics.bukkit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitMetricsTest {

    @TempDir
    Path serverDir;

    @Test
    void standaloneServerIsNotBehindProxy() throws IOException {
        write("spigot.yml", "settings:\n  bungeecord: false\n");
        assertFalse(BukkitMetrics.behindProxy(serverDir.toFile()));
    }

    @Test
    void missingConfigsAreNotAProxy() {
        assertFalse(BukkitMetrics.behindProxy(serverDir.toFile()));
    }

    @Test
    void detectsBungeeCord() throws IOException {
        write("spigot.yml", "settings:\n  bungeecord: true\n");
        assertTrue(BukkitMetrics.behindProxy(serverDir.toFile()));
    }

    @Test
    void detectsVelocityOnLegacyPaper() throws IOException {
        write("paper.yml", "settings:\n  velocity-support:\n    enabled: true\n");
        assertTrue(BukkitMetrics.behindProxy(serverDir.toFile()));
    }

    @Test
    void detectsVelocityOnModernPaper() throws IOException {
        write("config/paper-global.yml", "proxies:\n  velocity:\n    enabled: true\n");
        assertTrue(BukkitMetrics.behindProxy(serverDir.toFile()));
    }

    private void write(String path, String content) throws IOException {
        Path file = serverDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }
}
