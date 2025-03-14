package org.location.lastLocation;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CommandTabCompleter implements TabCompleter {
    private final JavaPlugin plugin;

    public CommandTabCompleter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command cmd,
                                      @NotNull String label,
                                      @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        switch (cmd.getName().toLowerCase()) {
            case "agregar_mundo_lista_negra":
                if (args.length == 1) {
                    plugin.getServer().getWorlds().forEach(world -> suggestions.add(world.getName()));
                }
                break;

            case "quitar_mundo_lista_negra":
                if (args.length == 1) {
                    suggestions.addAll(plugin.getConfig().getStringList("worlds-blacklist"));
                }
                break;

            case "modificar_mensajes":
                if (args.length == 1) {
                    suggestions.add("teleportacion");
                }
                break;

            default:
                break;
        }

        return StringUtil.copyPartialMatches(args[args.length - 1], suggestions, new ArrayList<>());
    }
}
