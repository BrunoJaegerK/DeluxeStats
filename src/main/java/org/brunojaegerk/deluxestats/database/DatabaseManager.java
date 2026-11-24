package org.brunojaegerk.deluxestats.database;

import cn.nukkit.Player;
import org.brunojaegerk.deluxestats.DeluxeStats;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseManager {

    private final DeluxeStats plugin;
    private Connection connection;

    public DatabaseManager(DeluxeStats plugin) {
        this.plugin = plugin;
    }

    public synchronized void open() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        File folder = plugin.getDataFolder();

        if (!folder.exists() && !folder.mkdirs()) {
            throw new SQLException(
                    "No se pudo crear la carpeta de datos: "
                            + folder.getAbsolutePath()
            );
        }

        File databaseFile = new File(folder, "deluxestats");
        String databasePath = databaseFile.getAbsolutePath()
                .replace('\\', '/');
        String url = "jdbc:h2:file:" + databasePath;

        try {
            String driverName =
                    "org.brunojaegerk.deluxestats.libs.h2.Driver";

            Class<?> driverClass = Class.forName(
                    driverName,
                    true,
                    DatabaseManager.class.getClassLoader()
            );

            Driver driver = (Driver) driverClass
                    .getDeclaredConstructor()
                    .newInstance();

            Connection newConnection =
                    driver.connect(url, new Properties());

            if (newConnection == null) {
                throw new SQLException(
                        "H2 no aceptó la URL: " + url
                );
            }

            connection = newConnection;
            connection.setAutoCommit(true);

            initializeTables();

            plugin.getLogger().info(
                    "Base de datos H2 abierta correctamente."
            );
            plugin.getLogger().info(
                    "Archivo: "
                            + databaseFile.getAbsolutePath() + ".mv.db"
            );

        } catch (ReflectiveOperationException
                 | ClassCastException
                 | LinkageError exception) {
            close();

            throw new SQLException(
                    "No se pudo cargar el controlador H2 incluido "
                            + "en DeluxeStats.",
                    exception
            );
        } catch (SQLException exception) {
            close();

            throw new SQLException(
                    "No se pudo conectar o inicializar H2.",
                    exception
            );
        }
    }

    private void initializeTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {

            // Estadísticas históricas. No se reinician.
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS player_stats ("
                            + "uuid VARCHAR(36) PRIMARY KEY,"
                            + "name VARCHAR(64) NOT NULL,"
                            + "kills INTEGER NOT NULL DEFAULT 0,"
                            + "deaths INTEGER NOT NULL DEFAULT 0,"
                            + "ores_mined INTEGER NOT NULL DEFAULT 0,"
                            + "money_earned DOUBLE PRECISION NOT NULL DEFAULT 0,"
                            + "active_seconds BIGINT NOT NULL DEFAULT 0"
                            + ")"
            );

            // Añade las estadísticas PvP simples en bases de datos existentes.
            statement.executeUpdate(
                    "ALTER TABLE player_stats ADD COLUMN IF NOT EXISTS "
                            + "kills INTEGER NOT NULL DEFAULT 0"
            );
            statement.executeUpdate(
                    "ALTER TABLE player_stats ADD COLUMN IF NOT EXISTS "
                            + "deaths INTEGER NOT NULL DEFAULT 0"
            );
        }

        plugin.getLogger().info(
                "Tablas y comprobaciones de esquema completadas."
        );
    }

    public synchronized Connection getConnection() {
        return connection;
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().error(
                        "No se pudo cerrar la base de datos H2.",
                        exception
                );
            } finally {
                connection = null;
            }
        }
    }

    private void checkConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException(
                    "La conexión con H2 no está disponible."
            );
        }
    }

    public synchronized void ensurePlayer(Player player)
            throws SQLException {
        ensurePlayer(player.getUniqueId().toString(), player.getName());
    }

    private void ensurePlayer(String uuid, String name)
            throws SQLException {
        checkConnection();

        String updateSql =
                "UPDATE player_stats SET name = ? WHERE uuid = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(updateSql)) {
            statement.setString(1, name);
            statement.setString(2, uuid);

            if (statement.executeUpdate() == 0) {
                String insertSql =
                        "INSERT INTO player_stats (uuid, name) VALUES (?, ?)";

                try (PreparedStatement insert =
                             connection.prepareStatement(insertSql)) {
                    insert.setString(1, uuid);
                    insert.setString(2, name);
                    insert.executeUpdate();
                }
            }
        }
    }

    public synchronized void recordKill(Player player)
            throws SQLException {
        ensurePlayer(player);
        String sql = "UPDATE player_stats SET kills = kills + 1 WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.executeUpdate();
        }
    }

    public synchronized void recordDeath(Player player)
            throws SQLException {
        ensurePlayer(player);
        String sql = "UPDATE player_stats SET deaths = deaths + 1 WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.executeUpdate();
        }
    }

    public synchronized void recordOre(Player player)
            throws SQLException {
        ensurePlayer(player);

        String uuid = player.getUniqueId().toString();

        String sql =
                "UPDATE player_stats SET ores_mined = ores_mined + 1 "
                        + "WHERE uuid = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setString(1, uuid);
            statement.executeUpdate();
        }

}

    public synchronized void updateMoneyBalance(
            Player player,
            double balance
    ) throws SQLException {
        ensurePlayer(player);

        String sql =
                "UPDATE player_stats SET money_earned = ? WHERE uuid = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setDouble(1, balance);
            statement.setString(
                    2, player.getUniqueId().toString()
            );
            statement.executeUpdate();
        }
    }

    public synchronized void addActiveSeconds(
            Player player,
            long seconds
    ) throws SQLException {
        if (seconds <= 0L) {
            return;
        }

        ensurePlayer(player);

        String uuid = player.getUniqueId().toString();

        String sql =
                "UPDATE player_stats "
                        + "SET active_seconds = active_seconds + ? "
                        + "WHERE uuid = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {
            statement.setLong(1, seconds);
            statement.setString(2, uuid);
            statement.executeUpdate();
        }

}


}
