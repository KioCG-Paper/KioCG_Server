package com.kiocg.server.commands;

import com.kiocg.server.tasks.TPSBarTask;
import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Collection;
import java.util.Collections;

public class ChunkLoadCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chunkload")
                .requires(listener -> listener.hasPermission(Permissions.COMMANDS_GAMEMASTER, "bukkit.command.chunkload"))
                .executes(context -> execute(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .requires(listener -> listener.hasPermission(Permissions.COMMANDS_GAMEMASTER, "bukkit.command.chunkload.other"))
                        .executes(context -> execute(context.getSource(), EntityArgument.getPlayers(context, "targets")))
                )
        );
    }

    private static int execute(CommandSourceStack sender, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            LevelChunk chunk = player.level().getChunkIfLoaded(player.blockPosition());
            long chunkLoad = chunk != null ? chunk.getChunkLoad().getAverage() : 0L;
            Component component = MiniMessage.miniMessage().deserialize("<green>[<aqua>豆渣子<green>] <gold>玩家 <target> 的模拟区域负载: <totalload>(<totalloadpct>%), 所处区块负载: <chunkload>",
                    Placeholder.parsed("target", player.getGameProfile().name()),
                    Placeholder.component("totalload", Component.text(player.getNearbyChunkLoad(), NamedTextColor.WHITE)),
                    Placeholder.component("totalloadpct", TPSBarTask.instance().getChunkLoadColor(player, true)),
                    Placeholder.component("chunkload", Component.text(chunkLoad, NamedTextColor.WHITE)));
            sender.sendSuccess(() -> PaperAdventure.asVanilla(component), false);
        }
        return targets.size();
    }
}
