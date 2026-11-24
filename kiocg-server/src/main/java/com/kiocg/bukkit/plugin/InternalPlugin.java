package com.kiocg.bukkit.plugin;

import com.kiocg.server.tasks.BossBarTask;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InternalPlugin extends PluginBase {
    private static final String NAME = "InternalPlugin";
    private static final String VERSION = "1.0";
    private static final String MAIN_CLASS = "com.kiocg.bukkit.plugin.InternalPlugin";

    private final PluginDescriptionFile description = new PluginDescriptionFile(NAME, VERSION, MAIN_CLASS);
    private final Logger logger = Logger.getLogger(NAME);

    private boolean enabled = false;

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
        throw new UnsupportedOperationException("Not supported.");
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
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void reloadConfig() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void saveConfig() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void saveDefaultConfig() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void saveResource(final @NotNull String resourcePath, final boolean replace) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public @Nullable InputStream getResource(final @NotNull String filename) {
        return null; // 服务端 jar 中未提供本插件的资源
    }

    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String @NotNull [] args
    ) {
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String @NotNull [] args
    ) {
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
