package de.xxmiltenxx.plugin.database;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class HomeDatabase {
    private Connection connection;
    private final String dbPath = "plugins/TutorialPlugin/homes.db";

    public HomeDatabase() {
        ensureDatabaseFile();
        connect();
        createTables();
    }

    // Prüfen, ob die DB-Datei existiert, und ggf. das Verzeichnis erstellen
    private void ensureDatabaseFile() {
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();

        if (!parentDir.exists()) {
            parentDir.mkdirs(); // Verzeichnis erstellen
        }

        try {
            if (!dbFile.exists()) {
                dbFile.createNewFile(); // DB-Datei erstellen, falls nicht vorhanden
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Verbindung zur SQLite-Datenbank herstellen
    private void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Tabellen erstellen, falls sie nicht existieren
    private void createTables() {
        // Tabelle für Spielerdaten (max_homes)
        String sqlPlayerData = "CREATE TABLE IF NOT EXISTS player_data (" +
                "player_uuid TEXT PRIMARY KEY, " +
                "max_homes INTEGER DEFAULT 3" +
                ")";
        // Tabelle für Home-Punkte
        String sqlHomes = "CREATE TABLE IF NOT EXISTS homes (" +
                "player_uuid TEXT, " +
                "home_name TEXT, " +
                "world TEXT, " +
                "x DOUBLE, " +
                "y DOUBLE, " +
                "z DOUBLE, " +
                "yaw FLOAT, " +
                "pitch FLOAT, " +
                "PRIMARY KEY (player_uuid, home_name)" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlPlayerData);
            stmt.execute(sqlHomes);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Maximale Anzahl der Home-Punkte für einen Spieler setzen (in player_data)
    public void setMaxHomes(Player player, int maxHomes) {
        String sql = "INSERT OR REPLACE INTO player_data (player_uuid, max_homes) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setInt(2, maxHomes);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Maximale Anzahl der Home-Punkte für einen Spieler auslesen (aus player_data)
    public int getMaxHomes(Player player) {
        String sql = "SELECT max_homes FROM player_data WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("max_homes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 3; // Standardwert, falls kein Eintrag existiert
    }

    // Home-Punkt speichern oder überschreiben (in homes)
    public void setHome(Player player, String homeName, Location loc) {
        int maxHomes = getMaxHomes(player);
        if (getHomeCount(player) >= maxHomes && !homeExists(player, homeName)) {
            player.sendMessage("Du kannst nur " + maxHomes + " Homes setzen!");
            return;
        }
        String sql = "REPLACE INTO homes (player_uuid, home_name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, homeName);
            pstmt.setString(3, loc.getWorld().getName());
            pstmt.setDouble(4, loc.getX());
            pstmt.setDouble(5, loc.getY());
            pstmt.setDouble(6, loc.getZ());
            pstmt.setFloat(7, loc.getYaw());
            pstmt.setFloat(8, loc.getPitch());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Home-Punkt abrufen
    public Location getHome(Player player, String homeName) {
        String sql = "SELECT * FROM homes WHERE player_uuid = ? AND home_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, homeName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) return null;
                return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Alle Home-Namen eines Spielers abrufen
    public List<String> getHomes(Player player) {
        List<String> homes = new ArrayList<>();
        String sql = "SELECT home_name FROM homes WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                homes.add(rs.getString("home_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return homes;
    }

    // Home-Punkt löschen
    public void deleteHome(Player player, String homeName) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, homeName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Prüfen, ob ein Home existiert
    public boolean homeExists(Player player, String homeName) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ? AND home_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, homeName);
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Anzahl der Homes eines Spielers abrufen
    public int getHomeCount(Player player) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}