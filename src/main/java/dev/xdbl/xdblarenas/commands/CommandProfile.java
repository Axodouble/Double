package dev.xdbl.xdblarenas.commands;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.players.ArenaPlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandProfile implements CommandExecutor, TabCompleter {

    private final XDBLArena plugin;

    public CommandProfile(XDBLArena plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("xdbl.pvp")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.no_permission"))));
            return true;
        }

        Player player;
        if (args.length == 1) {
            player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(plugin.getConfig().getString("messages.bad_usage"))));
                return true;
            }
        } else {
            player = (Player) sender;
        }

        // Return the player's profile
        ArenaPlayer arenaPlayer = plugin.getPlayerManager().getArenaPlayer(player.getUniqueId());
        plugin.getConfig().getStringList("messages.profile").forEach(s -> sender.sendMessage(MiniMessage.miniMessage().deserialize(s
                .replace("%player%", player.getName())
                .replace("%elo%", String.valueOf(arenaPlayer.getElo()))
                .replace("%wins%", String.valueOf(arenaPlayer.wins()))
                .replace("%losses%", String.valueOf(arenaPlayer.losses()))
                .replace("%draws%", String.valueOf(arenaPlayer.draws()))
                .replace("%games%", String.valueOf(arenaPlayer.wins() + arenaPlayer.losses()))
                .replace("%pvpbanned%", arenaPlayer.pvpBanned() ? "<red>Yes" : "<green>No")
                .replace("%arenabanned%", arenaPlayer.arenaBanned() ? "<red>Yes" : "<green>No"))));



        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        // Return a list of all players
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return players;
        }
        return new ArrayList<>();
    }


}
