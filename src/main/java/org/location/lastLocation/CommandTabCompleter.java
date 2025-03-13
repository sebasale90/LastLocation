package org.location.lastLocation;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class CommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        // Autocompletado para el comando "/agregar_mundo_lista_negra"
        if (cmd.getName().equalsIgnoreCase("agregar_mundo_lista_negra")) {
            if (args.length == 1) {
                // Sugerir mundos disponibles (esto puede ser personalizado para tus necesidades)
                for (org.bukkit.World world : sender.getServer().getWorlds()) {
                    suggestions.add(world.getName());
                }
            }
        }

        // Autocompletado para el comando "/quitar_mundo_lista_negra"
        else if (cmd.getName().equalsIgnoreCase("quitar_mundo_lista_negra")) {
            if (args.length == 1) {
                // Obtener la lista negra desde la configuración
                List<String> blacklistedWorlds = sender.getServer().getPluginManager().getPlugin("LastLocation").getConfig().getStringList("worlds-blacklist");
                if (blacklistedWorlds != null) {
                    suggestions.addAll(blacklistedWorlds);  // Añadir mundos de la lista negra
                }
            }
        }

        // Autocompletado para el comando "/ver_lista_negra"
        else if (cmd.getName().equalsIgnoreCase("ver_lista_negra")) {
            if (args.length == 1) {
                suggestions.add("ver lista negra"); // Este comando no tiene argumentos adicionales
            }
        }

        // Autocompletado para el comando "/modificar_mensajes"
        else if (cmd.getName().equalsIgnoreCase("modificar_mensajes")) {
            if (args.length == 1) {
                suggestions.add("teleportacion"); // Sugerir el tipo de mensaje a modificar
            } else if (args.length == 2) {
                // Sugerir posibles mensajes que pueden ser modificados (aquí solo para teleportación)
                suggestions.add("¡Te hemos teletransportado!");
                suggestions.add("¡Teletransportación exitosa!");
            }
        }

        // Filtra y devuelve las sugerencias basadas en la entrada parcial del usuario
        return StringUtil.copyPartialMatches(args[args.length - 1], suggestions, new ArrayList<>());
    }
}
