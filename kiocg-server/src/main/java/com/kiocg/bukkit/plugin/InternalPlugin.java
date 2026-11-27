package com.kiocg.bukkit.plugin;

import com.kiocg.bukkit.plugin.listeners.GenerateTemplate;
import com.kiocg.server.tasks.BossBarTask;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InternalPlugin extends PluginBase implements Listener {
    private static final String NAME = "InternalPlugin";
    private static final String VERSION = "1.0";
    private static final String MAIN_CLASS = "com.kiocg.bukkit.plugin.InternalPlugin";

    private static final String CONFIG_FILE = "config.yml";
    private static final String RESOURCE_ROOT = "com/kiocg/bukkit/plugin/";

    private final PluginDescriptionFile description = new PluginDescriptionFile(NAME, VERSION, MAIN_CLASS);
    private final Logger logger = Logger.getLogger(NAME);
    private final File dataFolder = new File(Bukkit.getPluginsFolder(), NAME);
    private final File configFile = new File(this.dataFolder, CONFIG_FILE);

    private boolean enabled = false;
    private FileConfiguration config;

    public InternalPlugin() {
    }

    public static InternalPlugin getInstance() {
        return ((org.bukkit.craftbukkit.CraftServer) Bukkit.getServer()).internalPlugin;
    }

    @Override
    public void onLoad() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void onEnable() {
        if (this.enabled) {
            return;
        }
        this.enabled = true;
        try {
            this.saveDefaultConfig();
            this.reloadConfig();

            final PluginManager pluginManager = Bukkit.getPluginManager();

            ModifyRecipes.apply(this);
            pluginManager.registerEvent(ServerResourcesReloadedEvent.class, this, EventPriority.NORMAL, (_, _) -> ModifyRecipes.apply(this), this);
            pluginManager.registerEvents(new GenerateTemplate(), this);

            BossBarTask.startAll(); // KioCG - bossbar task
            this.logger.info("InternalPlugin Enabled.");
        } catch (Throwable t) {
            this.logger.log(Level.SEVERE, "InternalPlugin failed to enable.", t);
            Bukkit.shutdown();
        }
    }

    @Override
    public void onDisable() {
        if (!this.enabled) {
            return;
        }
        this.enabled = false;
        try {
            BossBarTask.stopAll(); // KioCG - bossbar task
            this.logger.info("InternalPlugin Disabled.");
        } catch (Throwable t) {
            this.logger.log(Level.SEVERE, "InternalPlugin failed to disable.", t);
        }
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    // ------------------------------------------------------------------
    // Plugin 契约
    // ------------------------------------------------------------------

    @Override
    public @NotNull File getDataFolder() {
        return this.dataFolder;
    }

    @Override
    public @NotNull PluginDescriptionFile getDescription() {
        return this.description;
    }

    @Override
    public @NotNull PluginMeta getPluginMeta() {
        return this.description;
    }

    @Override
    public @NotNull Logger getLogger() {
        return this.logger;
    }

    @Override
    public @NotNull Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public boolean isNaggable() {
        return false;
    }

    @Override
    public void setNaggable(final boolean canNag) {
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        if (this.config == null) {
            this.reloadConfig();
        }
        return this.config;
    }

    @Override
    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(this.configFile);

        try (InputStream defaults = this.getResource(CONFIG_FILE)) {
            if (defaults != null) {
                this.config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            this.logger.log(Level.WARNING, "Could not read default config from resource '" + RESOURCE_ROOT + CONFIG_FILE + "'", ex);
        }
    }

    @Override
    public void saveConfig() {
        try {
            if (!this.dataFolder.exists()) {
                this.dataFolder.mkdirs();
            }
            this.getConfig().save(this.configFile);
        } catch (IOException ex) {
            this.logger.log(Level.SEVERE, "Could not save config to " + this.configFile, ex);
        }
    }

    @Override
    public void saveDefaultConfig() {
        if (!this.configFile.exists()) {
            this.saveResource(CONFIG_FILE, false);
        }
    }

    @Override
    public void saveResource(final @NotNull String resourcePath, final boolean replace) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        final String path = resourcePath.replace('\\', '/');
        final File outFile = new File(this.dataFolder, path);
        if (outFile.exists() && !replace) {
            this.logger.warning("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            return;
        }

        final InputStream resource = this.getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("The embedded resource '" + RESOURCE_ROOT + path + "' cannot be found");
        }

        final File outDir = outFile.getParentFile();
        if (outDir != null && !outDir.exists()) {
            outDir.mkdirs();
        }

        try (InputStream in = resource; OutputStream out = new FileOutputStream(outFile)) {
            in.transferTo(out);
        } catch (IOException ex) {
            this.logger.log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    @Override
    public @Nullable InputStream getResource(final @NotNull String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        final URL url = this.getClass().getClassLoader().getResource(RESOURCE_ROOT + filename.replace('\\', '/'));
        if (url == null) {
            return null;
        }

        try {
            final URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args) {
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args) {
        return Collections.emptyList();
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(final @NotNull String worldName, final @Nullable String id) {
        return null;
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(final @NotNull String worldName, final @Nullable String id) {
        return null;
    }

    @Override
    public @NotNull PluginLoader getPluginLoader() {
        // Paper 的 registerEvents 走 PaperEventManager，不会用到 PluginLoader
        throw new UnsupportedOperationException("InternalPlugin has no PluginLoader");
    }

    @Override
    public @NotNull LifecycleEventManager<Plugin> getLifecycleManager() {
        // 内部插件不参与 Paper 的 lifecycle 事件，命令请直接注册到 Brigadier dispatcher
        throw new UnsupportedOperationException("InternalPlugin does not use Paper lifecycle events");
    }
}
