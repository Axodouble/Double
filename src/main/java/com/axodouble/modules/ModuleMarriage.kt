package com.axodouble.modules

import com.axodouble.Double
import com.axodouble.types.DoublePlayer
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.util.StringUtil

class ModuleMarriage(private val plugin: Double) : CommandExecutor, TabCompleter, Listener {

    private val invites: MutableMap<Player, Player> = mutableMapOf()

    init {
        plugin.getCommand("marry")?.setExecutor(this)
        plugin.getCommand("divorce")?.setExecutor(this)
        plugin.getCommand("marry")?.tabCompleter = this


        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) return true

        if (!sender.hasPermission("double.marry")) {
            plugin.noPermission(sender)
            return true
        }

        when (cmd.name) {
            "divorce" -> divorce(sender)
            "marry" -> {
                if (args.isEmpty()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: <white>/marry <player>"))
                    return true
                }

                val target = plugin.server.getPlayer(args[0])
                if (target == null) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found"))
                    return true
                }

                invite(sender, target)
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, label: String, args: Array<String>): List<String> {
        // Return all online players except the sender
        val tabOptions = Bukkit.getServer().onlinePlayers
                .filter { it.name != sender.name }
            .map { it.name }
            .toMutableList()

        val returnedOptions = mutableListOf<String>()
        StringUtil.copyPartialMatches(args.last(), tabOptions, returnedOptions)

        return returnedOptions
    }

    fun invite(sender: Player, target: Player) {
        val senderDoublePlayer = plugin.playerManager.getDoublePlayer(sender.uniqueId)
        val targetDoublePlayer = plugin.playerManager.getDoublePlayer(target.uniqueId)

        if (senderDoublePlayer.isMarried()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already married!"))
            return
        }
        if (targetDoublePlayer.isMarried()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>${target.name} is already married!"))
            return
        }

        if (invites.containsKey(sender)) {
            if (invites[sender] == target) {
                accept(sender, target)
                return
            }
        }

        invites[target] = sender
        plugin.server.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>${sender.name}<gray> has invited <green>${target.name}<gray> to marry them!"
        ))
        target.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>${sender.name}<gray> has invited you to marry them! Click <hover:show_text:'Click to accept the marriage proposal.'><click:run_command:/marry ${sender.name}>[<green>here<gray>]</click><gray> to accept."
        ))
    }

    fun accept(target: Player, sender: Player) {
        val senderDoublePlayer = plugin.playerManager.getDoublePlayer(sender.uniqueId)
        val targetDoublePlayer = plugin.playerManager.getDoublePlayer(target.uniqueId)

        if (senderDoublePlayer.isMarried()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already married!"))
            target.sendMessage(MiniMessage.miniMessage().deserialize("<red>${sender.name} is already married!"))
            return
        }
        if (targetDoublePlayer.isMarried()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>${target.name} is already married!"))
            target.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already married!"))
            return
        }
        if (invites[target] != sender) {
            return
        }

        plugin.server.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>${target.name}<gray> has accepted <green>${sender.name}<gray>'s marriage proposal!"
        ))

        targetDoublePlayer.marry(sender.name)
        senderDoublePlayer.marry(target.name)
        invites.remove(target)
    }

    fun decline(target: Player, sender: Player): Int {
        val senderDoublePlayer = plugin.playerManager.getDoublePlayer(sender.uniqueId)
        val targetDoublePlayer = plugin.playerManager.getDoublePlayer(target.uniqueId)

        if (senderDoublePlayer.isMarried()) {
            return 1
        }
        if (targetDoublePlayer.isMarried()) {
            return 2
        }
        if (invites[target] != sender) {
            return 3
        }

        plugin.server.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>${target.name}<gray> has declined <green>${sender.name}<gray>'s marriage proposal!"
        ))
        invites.remove(target)
        return 4
    }

    fun divorce(player: Player) {
        val doublePlayer = plugin.playerManager.getDoublePlayer(player.uniqueId)
        val doublePartner = plugin.playerManager.getDoublePlayer(doublePlayer.getPartner() ?: return)

        doublePlayer.divorce()
        doublePartner.divorce()

        plugin.server.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>${player.name} has divorced ${doublePartner.name}."
        ))
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        invites.remove(event.player)
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val rightClicked = event.rightClicked as? Player ?: return
        if (event.player.uniqueId == rightClicked.uniqueId || !event.player.isSneaking) return

                val doublePlayer = plugin.playerManager.getDoublePlayer(event.player.uniqueId)
        if (doublePlayer.isMarried() && rightClicked.name == doublePlayer.getMarriedName()) {
            // Spawn a bunch of hearts
            spawnHeartsAroundPlayer(rightClicked)
            spawnHeartsAroundPlayer(event.player)
        }
    }

    private fun spawnHeartsAroundPlayer(player: Player) {
        val world = player.world
        val playerLocation = player.location

        val heartsToSpawn = (Math.random() * 3).toInt() + 2
        repeat(heartsToSpawn) {
            val angle = Math.random() * Math.PI * 2
            val radius = 0.5
            val x = playerLocation.x + Math.cos(angle) * radius
            val y = playerLocation.y + (Math.random() * 0.3) + 1.5
            val z = playerLocation.z + Math.sin(angle) * radius

            val particleLocation = Location(world, x, y, z)
            world.spawnParticle(Particle.HEART, particleLocation, 1)
        }
    }

    fun getRequests(player: Player): Collection<String> {
        return invites.filter { it.value == player }.keys.map { it.name }
    }
}
