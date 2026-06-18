package com.kiocg.server.tasks;

import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.configuration.WorldConfiguration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class TPSBarTask extends BossBarTask {
    private static final Predicate<Double> GOOD_TPS = value -> Math.round(value) >= 20;
    private static final Predicate<Double> Medium_TPS = value -> value >= 15;
    private static final Predicate<Double> GOOD_MSPT = value -> value <= 50;
    private static final Predicate<Double> Medium_MSPT = value -> value <= 65;
    private static final Predicate<Integer> GOOD_PING = value -> value < 100;
    private static final Predicate<Integer> Medium_PING = value -> value < 200;

    private static TPSBarTask instance;
    private double tps = 20.0D;
    private double mspt = 0.0D;
    private int tick = 0;

    public static TPSBarTask instance() {
        if (instance == null) {
            instance = new TPSBarTask();
        }
        return instance;
    }

    @Override
    BossBar createBossBar() {
        return BossBar.bossBar(Component.text(""), 0.0F, GlobalConfiguration.get().kiocg.tpsBar.progressColorGood, GlobalConfiguration.get().kiocg.tpsBar.progressOverlay);
    }

    @Override
    void updateBossBar(BossBar bossbar, Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        bossbar.progress(getBossBarProgress((float) serverPlayer.getNearbyChunkLoad(), (float) serverPlayer.level().paperConfig().kiocg.chunkLoad.mediumPhase));
        bossbar.color(getBossBarColor(serverPlayer));
        bossbar.name(MiniMessage.miniMessage().deserialize(GlobalConfiguration.get().kiocg.tpsBar.title,
                Placeholder.component("loadpct", getChunkLoadColor(serverPlayer, true)),
                Placeholder.component("tps", getTPSColor()),
                Placeholder.component("mspt", getMSPTColor()),
                Placeholder.component("ping", getPingColor(player.getPing()))
        ));
    }

    @Override
    public void run() {
        if (++tick < GlobalConfiguration.get().kiocg.tpsBar.tickInterval) {
            return;
        }
        tick = 0;

        this.tps = Math.min(MinecraftServer.getServer().getTPS()[0], 20.0D);
        this.mspt = Bukkit.getAverageTickTime();

        super.run();
    }

    private float getBossBarProgress(float dividend, float divisor) {
        return Math.clamp(dividend / divisor, 0.0F, 1.0F);
    }

    private BossBar.Color getBossBarColor(ServerPlayer player) {
        long load = player.getNearbyChunkLoad();
        WorldConfiguration.Kiocg.ChunkLoad loadCfg = player.level().paperConfig().kiocg.chunkLoad;
        if (load < loadCfg.mediumPhase) {
            return GlobalConfiguration.get().kiocg.tpsBar.progressColorGood;
        } else if (load < loadCfg.lowPhase) {
            return GlobalConfiguration.get().kiocg.tpsBar.progressColorMedium;
        } else {
            return GlobalConfiguration.get().kiocg.tpsBar.progressColorLow;
        }
    }

    private Component getTPSColor() {
        String color = getColor(tps, GOOD_TPS, Medium_TPS);
        return MiniMessage.miniMessage().deserialize(color, Placeholder.parsed("text", String.format("%.2f", tps)));
    }

    private Component getMSPTColor() {
        String color = getColor(mspt, GOOD_MSPT, Medium_MSPT);
        return MiniMessage.miniMessage().deserialize(color, Placeholder.parsed("text", String.format("%.2f", mspt)));
    }

    private Component getPingColor(int ping) {
        String color = getColor(ping, GOOD_PING, Medium_PING);
        return MiniMessage.miniMessage().deserialize(color, Placeholder.parsed("text", String.format("%s", ping)));
    }

    public Component getChunkLoadColor(ServerPlayer player, boolean pct) {
        WorldConfiguration.Kiocg.ChunkLoad loadCfg = player.level().paperConfig().kiocg.chunkLoad;
        Predicate<Long> GOOD_LOAD = value -> value < loadCfg.mediumPhase;
        Predicate<Long> MEDIUM_LOAD = value -> value < loadCfg.lowPhase;
        long load = player.getNearbyChunkLoad();
        String color = getColor(load, GOOD_LOAD, MEDIUM_LOAD);
        if (pct) {
            load = load * 100 / loadCfg.mediumPhase;
        }
        return MiniMessage.miniMessage().deserialize(color, Placeholder.parsed("text", String.format("%s", load)));
    }

    private <T> String getColor(T value, Predicate<T> goodValue, Predicate<T> mediumValue) {
        if (goodValue.test(value)) {
            return GlobalConfiguration.get().kiocg.tpsBar.textColorGood;
        } else if (mediumValue.test(value)) {
            return GlobalConfiguration.get().kiocg.tpsBar.textColorMedium;
        } else {
            return GlobalConfiguration.get().kiocg.tpsBar.textColorLow;
        }
    }
}
