package org.quipumetrics.bukkit;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.quipumetrics.core.Metrics;

import java.io.File;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Entry point for Bukkit, Spigot, Paper and Folia plugins, 1.8.8 and newer.
 *
 * <pre>
 * Metrics metrics = BukkitMetrics.create(this, 1234);
 * metrics.addChart(new SimplePieChart("language", () -&gt; getConfig().getString("lang")));
 * </pre>
 *
 * Collection stops by itself when the plugin is disabled, /reload included.
 */
@UtilityClass
public class BukkitMetrics {

    private static final String FOLIA_SERVER = "io.papermc.paper.threadedregions.RegionizedServer";
    private static final String FOLIA_SCHEDULER = "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler";

    /**
     * Builds and starts the client. Call from onEnable.
     */
    public Metrics create(Plugin plugin, int serviceId) {
        boolean behindProxy = behindProxy(new File("."));

        Metrics metrics = Metrics.builder()
                .serviceId(serviceId)
                .serviceVersion(plugin.getDescription().getVersion())
                .platform("bukkit")
                .platformVersion(Bukkit::getVersion)
                // Shared by every plugin on the host, so the host keeps one serverUUID.
                .configFolder(new File(plugin.getDataFolder().getParentFile(), "Quipu"))
                .playerCount(() -> Bukkit.getOnlinePlayers().size())
                .onlineMode(() -> Bukkit.getOnlineMode() ? 1 : behindProxy ? -1 : 0)
                .taskRunner(taskRunner(plugin))
                .logger(plugin.getLogger())
                .build();
        metrics.start();

        // Without this a /reload leaves the old scheduler running next to the new one.
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onDisable(PluginDisableEvent event) {
                if (event.getPlugin() == plugin) metrics.shutdown();
            }
        }, plugin);
        return metrics;
    }

    /**
     * A backend behind BungeeCord or Velocity runs in offline mode while the proxy
     * authenticates, so its own setting says nothing about the network. The proxy
     * reports the real value itself.
     */
    boolean behindProxy(File serverDir) {
        return enabled(serverDir, "spigot.yml", "settings.bungeecord")
                || enabled(serverDir, "paper.yml", "settings.velocity-support.enabled")
                || enabled(serverDir, "config/paper-global.yml", "proxies.velocity.enabled");
    }

    private boolean enabled(File serverDir, String path, String key) {
        File file = new File(serverDir, path);
        return file.isFile() && YamlConfiguration.loadConfiguration(file).getBoolean(key);
    }

    @SneakyThrows
    private Consumer<Runnable> taskRunner(Plugin plugin) {
        if (!isFolia()) return task -> Bukkit.getScheduler().runTask(plugin, task);

        // Folia has no main thread and rejects the Bukkit scheduler. Its global
        // region scheduler is not in the 1.8.8 API this module compiles against.
        Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
        Method execute = Class.forName(FOLIA_SCHEDULER).getMethod("execute", Plugin.class, Runnable.class);
        return task -> invoke(execute, scheduler, plugin, task);
    }

    private boolean isFolia() {
        try {
            Class.forName(FOLIA_SERVER);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SneakyThrows
    private void invoke(Method method, Object target, Object... args) {
        method.invoke(target, args);
    }
}
