package org.location.lastLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class SQLite {
    private final JavaPlugin plugin;
    private Connection connection;

    public SQLite(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Conectar a la base de datos SQLite
    public void connectToDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();

            // Crear la carpeta del plugin si no existe
            if (!dataFolder.exists()) {
                if (dataFolder.mkdirs()) {
                    plugin.getLogger().info("Carpeta del plugin creada en: " + dataFolder.getAbsolutePath());
                } else {
                    plugin.getLogger().severe("No se pudo crear la carpeta del plugin: " + dataFolder.getAbsolutePath());
                }
            }

            // Conectar a la base de datos SQLite
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/locations.db");
            createTable();
            plugin.getLogger().info("Conexión a la base de datos SQLite establecida.");
        } catch (SQLException e) {
            plugin.getLogger().severe("No se pudo conectar a la base de datos SQLite: " + e.getMessage());
            logStackTrace(e);
        }
    }

    // Verifica si la conexión está abierta, y si no, intenta reconectar
    private void checkConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connectToDatabase();
        }
    }

    // Crear la tabla si no existe
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_locations (" +
                "uuid TEXT NOT NULL, " +
                "world TEXT NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "yaw FLOAT NOT NULL, " +
                "pitch FLOAT NOT NULL, " +
                "PRIMARY KEY (uuid, world));";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear la tabla de ubicaciones: " + e.getMessage());
            logStackTrace(e);
        }
    }

    // Guardar la ubicación de un jugador
    public void saveLocation(String uuid, String world, Location location) {
        try {
            checkConnection();

            if (isValidUUID(uuid) && isValidWorldName(world)) {
                String query = "REPLACE INTO player_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, uuid);
                    stmt.setString(2, world);
                    stmt.setDouble(3, location.getX());
                    stmt.setDouble(4, location.getY());
                    stmt.setDouble(5, location.getZ());
                    stmt.setFloat(6, location.getYaw());
                    stmt.setFloat(7, location.getPitch());
                    stmt.executeUpdate();
                }
            } else {
                plugin.getLogger().warning("Entrada no válida: UUID o nombre del mundo incorrecto.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al guardar la ubicación de " + uuid + " en el mundo " + world + ": " + e.getMessage());
            logStackTrace(e);
        }
    }

    // Obtener la última ubicación de un jugador
    public Location getLastLocation(String uuid, String world) {
        try {
            checkConnection();

            if (isValidUUID(uuid) && isValidWorldName(world)) {
                String query = "SELECT x, y, z, yaw, pitch FROM player_locations WHERE uuid = ? AND world = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, uuid);
                    stmt.setString(2, world);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        double x = rs.getDouble("x");
                        double y = rs.getDouble("y");
                        double z = rs.getDouble("z");
                        float yaw = rs.getFloat("yaw");
                        float pitch = rs.getFloat("pitch");

                        World bukkitWorld = Bukkit.getWorld(world);
                        if (bukkitWorld != null) {
                            return new Location(bukkitWorld, x, y, z, yaw, pitch);
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("Entrada no válida: UUID o nombre del mundo incorrecto.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al obtener la última ubicación de " + uuid + " en el mundo " + world + ": " + e.getMessage());
            logStackTrace(e);
        }
        return null;
    }

    // Validación de UUID
    public boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Validación de nombre de mundo
    public boolean isValidWorldName(String worldName) {
        return worldName != null && !worldName.trim().isEmpty();
    }

    // Cerrar la conexión con la base de datos
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Conexión a SQLite cerrada correctamente.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al cerrar la conexión con la base de datos: " + e.getMessage());
            logStackTrace(e);
        }
    }

    // Método para registrar el stack trace de errores
    private void logStackTrace(Exception e) {
        for (StackTraceElement element : e.getStackTrace()) {
            plugin.getLogger().warning(element.toString());
        }
    }
}
