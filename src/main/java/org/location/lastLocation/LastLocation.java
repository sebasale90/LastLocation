package org.location.lastLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.location.lastLocation.SQLite;  // Importación de la clase SQLite

import java.util.List;

public class LastLocation extends JavaPlugin implements Listener {
    private SQLite sqlite;

    @Override
    public void onEnable() {
        // Crear una nueva instancia de SQLite y conectar a la base de datos
        sqlite = new SQLite(this);
        sqlite.connectToDatabase();

        // Cargar la configuración por defecto
        saveDefaultConfig();

        // Registrar eventos
        Bukkit.getPluginManager().registerEvents(this, this);

        // Registrar el TabCompleter para los comandos
        getCommand("agregar mundo a lista negra").setTabCompleter(new CommandTabCompleter());
        getCommand("quitar mundo de lista negra").setTabCompleter(new CommandTabCompleter());
        getCommand("ver lista negra").setTabCompleter(new CommandTabCompleter());
        getCommand("modificar mensajes").setTabCompleter(new CommandTabCompleter());
    }

    @Override
    public void onDisable() {
        // Cerrar la conexión a la base de datos cuando el plugin se desactiva
        sqlite.closeConnection();
    }

    // Guardar la ubicación cuando el jugador se desconecta
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        savePlayerLocation(player);
    }

    // Teletransportar al jugador a su última ubicación cuando ingresa al mundo
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        teleportToLastLocation(player);
    }

    public void savePlayerLocation(Player player) {
        World world = player.getWorld();
        String worldName = world.getName();

        // Verificar si el mundo está en la lista negra
        if (isWorldBlacklisted(worldName)) {
            return;
        }

        // Obtener la ubicación y guardarla en la base de datos
        Location location = player.getLocation();
        sqlite.saveLocation(player.getUniqueId().toString(), worldName, location);
    }

    public void teleportToLastLocation(Player player) {
        String worldName = player.getWorld().getName();

        // Verificar si el mundo está en la lista negra
        if (isWorldBlacklisted(worldName)) {
            return;
        }

        // Obtener la última ubicación desde la base de datos
        Location lastLocation = sqlite.getLastLocation(player.getUniqueId().toString(), worldName);
        if (lastLocation != null) {
            player.teleport(lastLocation);
        }
    }

    // Verificar si el mundo está en la lista negra
    private boolean isWorldBlacklisted(String worldName) {
        List<String> blacklistedWorlds = getConfig().getStringList("worlds-blacklist");
        return blacklistedWorlds.contains(worldName);
    }

    // Comandos para agregar, quitar y ver mundos en la lista negra, y modificar mensajes
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("agregar mundo a lista negra") && sender.hasPermission("lastlocation.admin")) {
            if (args.length == 1) {
                String worldName = args[0];
                List<String> blacklistedWorlds = getConfig().getStringList("worlds-blacklist");

                if (!blacklistedWorlds.contains(worldName)) {
                    blacklistedWorlds.add(worldName);
                    getConfig().set("worlds-blacklist", blacklistedWorlds);
                    saveConfig();
                    sender.sendMessage("§aMundo '" + worldName + "' ha sido añadido a la lista negra.");
                } else {
                    sender.sendMessage("§cEl mundo '" + worldName + "' ya está en la lista negra.");
                }
            } else {
                sender.sendMessage("§cUso incorrecto del comando. Usa: /agregar mundo a lista negra <world_name>");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("quitar mundo de lista negra") && sender.hasPermission("lastlocation.admin")) {
            if (args.length == 1) {
                String worldName = args[0];
                List<String> blacklistedWorlds = getConfig().getStringList("worlds-blacklist");

                if (blacklistedWorlds.contains(worldName)) {
                    blacklistedWorlds.remove(worldName);
                    getConfig().set("worlds-blacklist", blacklistedWorlds);
                    saveConfig();
                    sender.sendMessage("§aMundo '" + worldName + "' ha sido eliminado de la lista negra.");
                } else {
                    sender.sendMessage("§cEl mundo '" + worldName + "' no está en la lista negra.");
                }
            } else {
                sender.sendMessage("§cUso incorrecto del comando. Usa: /quitar mundo de lista negra <world_name>");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("ver lista negra") && sender.hasPermission("lastlocation.admin")) {
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

        if (cmd.getName().equalsIgnoreCase("modificar mensajes") && sender.hasPermission("lastlocation.admin")) {
            if (args.length == 2) {
                String messageType = args[0];
                String newMessage = args[1];

                if (messageType.equalsIgnoreCase("teleportacion")) {
                    getConfig().set("messages.teleportation", newMessage);
                    saveConfig();
                    sender.sendMessage("§aMensaje de teleportación modificado exitosamente.");
                } else {
                    sender.sendMessage("§cTipo de mensaje desconocido. Usa 'teleportacion'.");
                }
            } else {
                sender.sendMessage("§cUso incorrecto del comando. Usa: /modificar mensajes <tipo> <nuevo_mensaje>");
            }
            return true;
        }

        return false;
    }
}
