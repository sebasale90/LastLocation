import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class SQLite {
    private final JavaPlugin plugin;
    private Connection connection;

    public SQLite(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Conectar a la base de datos SQLite
    public void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/locations.db");
            createTable();
        } catch (SQLException e) {
            plugin.getLogger().warning("No se pudo conectar a la base de datos SQLite: " + e.getMessage());
            logStackTrace(e);
        }
    }

    // Verifica si la conexión está abierta, y si no, intenta reconectar
    private void checkConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connectToDatabase();  // Reintenta la conexión si no está abierta
        }
    }

    // Crear la tabla si no existe
    private void createTable() {
        try (Statement stmt = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS player_locations (" +
                    "uuid TEXT NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL, " +
                    "PRIMARY KEY (uuid, world));";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al crear la tabla de ubicaciones: " + e.getMessage());
            logStackTrace(e);
        }
    }

    // Guardar la ubicación de un jugador
    public void saveLocation(String uuid, String world, Location location) {
        try {
            checkConnection();  // Verifica la conexión antes de continuar

            // Validamos las entradas del usuario para asegurarnos de que son válidas.
            if (!isValidUUID(uuid) || !isValidWorldName(world)) {
                plugin.getLogger().warning("Entrada no válida: UUID o nombre del mundo incorrecto.");
                return;  // Detenemos la ejecución si las entradas no son válidas.
            }

            // Primero, verifica si el jugador ya tiene una ubicación guardada
            String query = "SELECT COUNT(*) FROM player_locations WHERE uuid = ? AND world = ?";
            try (PreparedStatement selectStmt = connection.prepareStatement(query)) {
                selectStmt.setString(1, uuid);  // Establece el parámetro de forma segura
                selectStmt.setString(2, world); // Establece el parámetro de forma segura
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    // Si ya existe una entrada, realiza una actualización
                    String updateQuery = "UPDATE player_locations SET x = ?, y = ?, z = ?, yaw = ?, pitch = ? WHERE uuid = ? AND world = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setDouble(1, location.getX());
                        updateStmt.setDouble(2, location.getY());
                        updateStmt.setDouble(3, location.getZ());
                        updateStmt.setFloat(4, location.getYaw());
                        updateStmt.setFloat(5, location.getPitch());
                        updateStmt.setString(6, uuid);
                        updateStmt.setString(7, world);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Si no existe, realiza una inserción
                    String insertQuery = "INSERT INTO player_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, uuid);
                        insertStmt.setString(2, world);
                        insertStmt.setDouble(3, location.getX());
                        insertStmt.setDouble(4, location.getY());
                        insertStmt.setDouble(5, location.getZ());
                        insertStmt.setFloat(6, location.getYaw());
                        insertStmt.setFloat(7, location.getPitch());
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al guardar la ubicación de " + uuid + " en el mundo " + world + ": " + e.getMessage());
            logStackTrace(e);
        }
    }

    // Obtener la última ubicación de un jugador
    public Location getLastLocation(String uuid, String world) {
        try {
            checkConnection();  // Verifica la conexión antes de continuar

            // Validamos las entradas del usuario
            if (!isValidUUID(uuid) || !isValidWorldName(world)) {
                plugin.getLogger().warning("Entrada no válida: UUID o nombre del mundo incorrecto.");
                return null;
            }

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
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al obtener la última ubicación de " + uuid + " en el mundo " + world + ": " + e.getMessage());
            logStackTrace(e);
        }
        return null; // Si no se encuentra la ubicación, retorna null
    }

    // Validación de UUID
    public boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);  // Si la conversión falla, no es un UUID válido
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Validación de nombre de mundo
    public boolean isValidWorldName(String worldName) {
        // Aquí puedes agregar reglas más específicas para validar el nombre del mundo si es necesario
        return worldName != null && !worldName.isEmpty();
    }

    // Método para cerrar la conexión
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al cerrar la conexión con la base de datos: " + e.getMessage());
            logStackTrace(e);
        }
    }

    // Método para registrar el stack trace del error
    private void logStackTrace(Exception e) {
        for (StackTraceElement element : e.getStackTrace()) {
            plugin.getLogger().warning(element.toString());
        }
    }
}
