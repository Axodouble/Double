package dev.xdbl.commands.misc;

import dev.xdbl.Double;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandSit implements CommandExecutor, TabCompleter {

    private final Double plugin;

    public CommandSit(Double plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("sit")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        plugin.getChairManager().sit(player, player.getLocation().add(0, -0.3, 0));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return new ArrayList<>();
    }
}
