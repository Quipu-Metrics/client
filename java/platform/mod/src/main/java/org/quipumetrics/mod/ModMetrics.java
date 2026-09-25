package org.quipumetrics.mod;

import lombok.experimental.UtilityClass;
import org.quipumetrics.core.Metrics;

import java.io.File;
import java.util.logging.Logger;

/**
 * Entry point for Fabric, Quilt, Forge and NeoForge mods.
 * <p>
 * The mod supplies the two values that only it can read, because it is the side
 * compiled against Minecraft with the right mappings for its own version:
 *
 * <pre>
 * Metrics metrics = ModMetrics.builder(1234, "1.0.0")
 *         .playerCount(() -&gt; server.getPlayerCount())
 *         .onlineMode(() -&gt; server.usesAuthentication() ? 1 : 0)
 *         .build();
 * metrics.start();
 * </pre>
 *
 * Those method names belong to the mod's own mappings. Quipu never sees them.
 */
@UtilityClass
public class ModMetrics {

    /**
     * @return a builder with the loader, Minecraft version, config folder and task
     * runner already filled in. The caller still has to supply playerCount and
     * onlineMode.
     */
    public Metrics.MetricsBuilder builder(int serviceId, String serviceVersion) {
        return Metrics.builder()
                .serviceId(serviceId)
                .serviceVersion(serviceVersion)
                .platform(LoaderInfo.platform())
                .platformVersion(LoaderInfo::minecraftVersion)
                // Every loader resolves config against the server directory, so one
                // relative path is correct everywhere and no loader API is needed.
                .configFolder(new File("config/quipu"))
                // Mods have no main thread requirement for reading a player count.
                // The value can be a tick stale, which does not matter once every
                // 30 minutes.
                .taskRunner(Runnable::run)
                .logger(Logger.getLogger("QuipuMetrics"));
    }
}
