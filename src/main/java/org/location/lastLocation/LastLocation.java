package org.location.lastLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Arrays;

public class LastLocation extends JavaPlugin implements Listener, CommandExecutor {
    private SQLite sqlite;

    @Override
    public void onEnable() {
        sqlite = new SQLite(this);
        sqlite.connectToDatabase();
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommand("agregar_mundo_lista_negra");
        registerCommand("quitar_mundo_lista_negra");
        registerCommand("ver_lista_negra");
        registerCommand("modificar_mensajes");
    }

    @Override
    public void onDisable() {
        sqlite.closeConnection();
    }

    private void registerCommand(String command) {
        if (getCommand(command) != null) {
            org.bukkit.command.PluginCommand cmd = getCommand(command);
            if (cmd != null) {
                cmd.setExecutor(this);
                cmd.setTabCompleter(new CommandTabCompleter(this));
                getLogger().info("Comando '" + command + "' registrado exitosamente.");
            }
        } else {
            getLogger().warning("El comando '" + command + "' no está registrado en plugin.yml");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        savePlayerLocation(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        teleportToLastLocation(event.getPlayer());
    }

    private void savePlayerLocation(Player player) {
        String worldName = player.getWorld().getName();
        if (!isWorldBlacklisted(worldName)) {
            sqlite.saveLocation(player.getUniqueId().toString(), worldName, player.getLocation());
        }
    }

    private void teleportToLastLocation(Player player) {
        String worldName = player.getWorld().getName();
        if (isWorldBlacklisted(worldName)) {
            player.sendMessage(getConfig().getString("messages.world_blacklisted", "Este mundo está en la lista negra, no se puede teletransportar."));
            return;
        }

        Location lastLocation = sqlite.getLastLocation(player.getUniqueId().toString(), worldName);
        if (lastLocation != null) {
            player.teleport(lastLocation);
            player.sendMessage(getConfig().getString("messages.teleportacion", "¡Te hemos teletransportado a tu última ubicación!"));
        } else {
            player.sendMessage(getConfig().getString("messages.no_last_location", "No se ha guardado ninguna ubicación para ti en este mundo."));
        }
    }

    private boolean isWorldBlacklisted(String worldName) {
        return getConfig().getStringList("worlds-blacklist").contains(worldName);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lastlocation.admin")) {
            sender.sendMessage("§cNo tienes permiso para ejecutar este comando.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "agregar_mundo_lista_negra":
                return handleBlacklistCommand(sender, args, true);
            case "quitar_mundo_lista_negra":
                return handleBlacklistCommand(sender, args, false);
            case "ver_lista_negra":
                return handleViewBlacklistCommand(sender);
            case "modificar_mensajes":
                handleModifyMessagesCommand(sender, args);
                return true;
            default:
                return false;
        }
    }

    private boolean handleBlacklistCommand(@NotNull CommandSender sender, @NotNull String[] args, boolean add) {
        if (args.length != 1) {
            sender.sendMessage("§cUso: /" + (add ? "agregar_mundo_lista_negra" : "quitar_mundo_lista_negra") + " <nombre_mundo>");
            return false;
        }

        List<String> blacklistedWorlds = getConfig().getStringList("worlds-blacklist");
        String worldName = args[0];

        if (add) {
            if (blacklistedWorlds.contains(worldName)) {
                sender.sendMessage("§cEl mundo '" + worldName + "' ya está en la lista negra.");
                return true;
            }
            blacklistedWorlds.add(worldName);
            sender.sendMessage("§aMundo '" + worldName + "' añadido a la lista negra.");
        } else {
            if (!blacklistedWorlds.remove(worldName)) {
                sender.sendMessage("§cEl mundo '" + worldName + "' no está en la lista negra.");
                return true;
            }
            sender.sendMessage("§aMundo '" + worldName + "' eliminado de la lista negra.");
        }

        getConfig().set("worlds-blacklist", blacklistedWorlds);
        saveConfig();
        return true;
    }

    private boolean handleViewBlacklistCommand(@NotNull CommandSender sender) {
        List<String> blacklistedWorlds = getConfig().getStringList("worlds-blacklist");

        if (blacklistedWorlds.isEmpty()) {
            sender.sendMessage("§cNo hay mundos en la lista negra.");
        } else {
            sender.sendMessage("§aMundos en la lista negra:");
            for (String world : blacklistedWorlds) {
                sender.sendMessage("§7- " + world);
            }
        }
        return true;
    }

    private void handleModifyMessagesCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /modificar_mensajes <tipo> <nuevo_mensaje>");
            return;
        }

        String messageType = args[0];
        String newMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (!messageType.equalsIgnoreCase("teleportacion")) {
            sender.sendMessage("§cTipo de mensaje desconocido. Usa 'teleportacion'.");
            return;
        }

        getConfig().set("messages.teleportacion", newMessage);
        saveConfig();
        sender.sendMessage("§aMensaje de teleportación modificado.");
    }
}
