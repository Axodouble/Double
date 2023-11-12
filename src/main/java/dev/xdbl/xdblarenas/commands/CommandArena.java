package dev.xdbl.xdblarenas.commands;

import dev.xdbl.xdblarenas.XDBLArena;
import dev.xdbl.xdblarenas.arenas.Arena;
import dev.xdbl.xdblarenas.players.ArenaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CommandArena implements CommandExecutor, TabCompleter {

    private final XDBLArena plugin;
    private final Map<String, Arena> creatingArenas = new HashMap<>();

    public CommandArena(XDBLArena plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!sender.hasPermission("xdbl.arena")){
            sender.sendMessage(plugin.getConfig().getString("messages.no_permission").replace("&", "§"));
            return true;
        }

        if (args.length == 0) {
            arenaHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            arenaList(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("scoreboard") || args[0].equalsIgnoreCase("top")) {
            Player p = (Player) sender;

            // Create and show a string list of the top 10 players with the highest elo
            List<String> top = new ArrayList<>();
            AtomicInteger i = new AtomicInteger();
            i.set(1);

            top.add(plugin.getConfig().getString("messages.scoreboard.top").replace("&", "§"));

            plugin.getPlayerManager().getArenaPlayers().stream().sorted(Comparator.comparingInt(ArenaPlayer::getElo).reversed()).limit(10).forEach(ap -> {
                String playerName = Bukkit.getOfflinePlayer(ap.getUUID()).getName();
                int elo = ap.getElo();

                String medal; // Default medal color for players outside the top 3

                // Check for 1st, 2nd, and 3rd place
                if (i.get() == 1) {
                    medal = "§6"; // Gold for 1st place
                } else if (i.get() == 2) {
                    medal = "§7"; // Silver for 2nd place
                } else if (i.get() == 3) {
                    medal = "§#cd7f32"; // Bronze for 3rd place
                } else {
                    medal = "§f"; // Default medal color for players outside the top 3
                }

                top.add(medal + i + " " + playerName + " §8- §7" + elo + " ELO (" + (ap.wins() + ap.losses()) + " games)");
                i.getAndIncrement();
            });

            // Send the top 10 players with the highest elo to the player
            top.forEach(p::sendMessage);

            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            arenaDelete(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) {
            arenaCreate(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("sp1")) {
            arenaSP1(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("sp2")) {
            arenaSP2(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("public")) {
            arenaPublic(sender, args);
            return true;
        }
        else {
            badUsage(sender);
            arenaHelp(sender);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("list", "delete", "public", "create", "sp1", "sp2", "top", "scoreboard");
        } else if (args.length == 2 && (
                args[0].equalsIgnoreCase("delete") ||
                        args[0].equalsIgnoreCase("public") ||
                        args[0].equalsIgnoreCase("sp1") ||
                        args[0].equalsIgnoreCase("sp2"))) {
            return Arrays.asList("<arena>");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            return Arrays.asList("<name>");
        } else if ((args.length == 3 && args[0].equalsIgnoreCase("public"))) {
            return Arrays.asList("true", "false");
        }
        return new ArrayList<>();
    }

    private void arenaSP1(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];

        Arena arena = creatingArenas.get(name);
        if (arena == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp1.not_found").replace("&", "§")
            );
            return;
        }
        if (arena.getOwner() != sender.getName()) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp1.not_yours").replace("&", "§")
            );
            return;
        }
        arena.setSpawnPoint1(((Player) sender).getLocation());
        sender.sendMessage(
                plugin.getConfig().getString("messages.arena.sp1.success").replace("&", "§")
        );
    }

    private void arenaSP2(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];

        Arena arena = creatingArenas.get(name);
        if (arena == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp2.not_found").replace("&", "§")
            );
            return;
        }
        if (arena.getOwner() != sender.getName()) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.sp2.not_yours").replace("&", "§")
            );
            return;
        }
        arena.setSpawnPoint2(((Player) sender).getLocation());
        sender.sendMessage(
                plugin.getConfig().getString("messages.arena.sp2.success").replace("&", "§")
        );
    }

    private void arenaCreate(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];

        // Check if the user is banned from creating arenas
        if (plugin.getPlayerManager().getArenaPlayer(((Player) sender).getUniqueId()).arenaBanned()){
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.create.banned").replace("&", "§")
            );
            return;
        }

        // Check if the string is the same as <name>, if so state the user should put a name
        if (name.equalsIgnoreCase("<name>")) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.create.no_name").replace("&", "§")
            );
            return;
        }
        // Check if the string is alphanumeric
        if (!name.matches("[a-zA-Z0-9]*")) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.create.alphanumeric").replace("&", "§")
            );
            return;
        }

        // Check if the arena already exists
        if (plugin.getArenaManager().getArenas().stream().filter(a -> a.getName().equalsIgnoreCase(name)).count() > 0) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.create.exists").replace("&", "§")
            );
            return;
        }

        File file = new File(plugin.getDataFolder(), "data/arenas/" + name + ".yml");

        Arena arena = new Arena(plugin, name, sender.getName(), ((Player) sender).getLocation(), ((Player) sender).getLocation(), false, file);
        creatingArenas.put(name, arena);

        arena.setSpawnPoint1(((Player) sender).getLocation());
        arena.setSpawnPoint2(((Player) sender).getLocation());
        arena.setReady(true);
        plugin.getArenaManager().addArena(arena);

        sender.sendMessage(
                plugin.getConfig().getString("messages.arena.create.success").replace("&", "§")
        );
    }

    private void arenaDelete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().getArenas().stream().filter(a -> a.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (arena == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.delete.not_found").replace("&", "§")
            );
            return;
        }
        if (!arena.getOwner().equals(sender.getName())) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.delete.not_yours").replace("&", "§")
            );
            return;
        }
        if (arena.getState() != Arena.ArenaState.WAITING) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.delete.running").replace("&", "§")
            );
            return;
        }

        arena.delete();
        plugin.getArenaManager().removeArena(arena);

        sender.sendMessage(
                plugin.getConfig().getString("messages.arena.delete.delete").replace("&", "§")
        );
    }

    private void arenaPublic(CommandSender sender, String[] args) {
        if (args.length == 1) {
            badUsage(sender);
            return;
        }

        String name = args[1];
        Arena arena = plugin.getArenaManager().getArenas().stream().filter(a -> a.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (arena == null) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.public_command.not_found").replace("&", "§")
            );
            return;
        }
        if (!arena.getOwner().equals(sender.getName())) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.public_command.not_yours").replace("&", "§")
            );
            return;
        }

        boolean isPublic = arena.isPublic();

        if (isPublic) {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.public_command.success_private").replace("&", "§")
            );
        } else {
            sender.sendMessage(
                    plugin.getConfig().getString("messages.arena.public_command.success_public").replace("&", "§")
            );
        }

        arena.setPublic(!isPublic);
    }

    private void badUsage(CommandSender sender) {
        sender.sendMessage(plugin.getConfig().getString("messages.bad_usage").replace("&", "§"));
    }

    private void arenaList(CommandSender sender) {
        List<Arena> arenas = plugin.getArenaManager().getArenas().stream().filter(arena -> arena.getOwner().equals(sender.getName())).collect(Collectors.toList());
        plugin.getConfig().getStringList("messages.arena.list").forEach(s -> {
            if (s.contains("%arenas")) {
                arenas.forEach(a -> {
                    sender.sendMessage(s.replace("%arenas", a.getName()).replace("&", "§") +
                            (a.getSpawnPoint1() != null ? " §8(" + a.getSpawnPoint1().getBlockX() + ", " + a.getSpawnPoint1().getBlockY() + ", " + a.getSpawnPoint1().getBlockZ() + ")" : ""));
                });
            } else {
                sender.sendMessage(s.replace("&", "§"));
            }
        });
    }

    private void arenaHelp(CommandSender sender) {
        plugin.getConfig().getStringList("messages.arena.help").forEach(s -> {
            sender.sendMessage(s.replace("&", "§"));
        });
    }
}
