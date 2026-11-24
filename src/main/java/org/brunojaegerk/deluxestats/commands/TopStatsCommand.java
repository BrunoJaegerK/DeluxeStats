package org.brunojaegerk.deluxestats.commands;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import org.brunojaegerk.deluxestats.DeluxeStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TopStatsCommand extends Command {

    private final DeluxeStats plugin;

    public TopStatsCommand(DeluxeStats plugin) {
        super(
                "topstats",
                "Muestra los mejores jugadores del servidor",
                "/topstats <ores|playtime|reset>"
        );
        this.plugin = plugin;
    }

    @Override
    public boolean execute(
            CommandSender sender,
            String commandLabel,
            String[] args
    ) {
        if (args.length != 1) {
            sender.sendMessage("§b§lDeluxeStats §8» §fUso: §a/topstats <ores|playtime|reset>");
            return true;
        }

        String type = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("reset".equals(type)) {
            return resetSeasonAndTops(sender);
        }

        String column;
        String title;
        String valueLabel;

        switch (type) {
            case "ores":
                column = "ores_mined";
                title = "Más minerales extraídos";
                valueLabel = "minerales";
                break;

            case "playtime":
                column = "active_seconds";
                title = "Mayor tiempo jugado";
                valueLabel = "tiempo activo";
                break;

            default:
                sender.sendMessage("§b§lDeluxeStats §8» §cCategoría desconocida.");
                sender.sendMessage("§7Categorías: §aores, playtime, reset");
                return true;
        }

        String sql = "SELECT name, " + column
                + " AS stat_value FROM player_stats "
                + "ORDER BY " + column + " DESC, name ASC LIMIT 10";

        sender.sendMessage("§8§m--------------------------------");
        sender.sendMessage("§b§lDeluxeStats §8» §fTop 10: §a" + title);
        sender.sendMessage(" ");

        try {
            Connection connection = plugin.getDatabaseManager().getConnection();

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {

                int position = 0;

                while (result.next()) {
                    position++;

                    String playerName = result.getString("name");
                    long value = result.getLong("stat_value");

                    String formattedValue;

                    if ("playtime".equals(type)) {
                        formattedValue = formatPlaytime(value);
                    } else {
                        formattedValue = String.valueOf(value);
                    }

                    sender.sendMessage(
                            "§e#" + position
                                    + " §f" + playerName
                                    + " §8» §a" + formattedValue
                                    + ("playtime".equals(type)
                                    ? ""
                                    : " §7" + valueLabel)
                    );
                }

                if (position == 0) {
                    sender.sendMessage("§7Todavía no hay estadísticas registradas.");
                }
            }

        } catch (SQLException exception) {
            sender.sendMessage("§cNo se pudo consultar el ranking.");
            plugin.getLogger().error(
                    "Error consultando el ranking " + type,
                    exception
            );
            return true;
        }

        sender.sendMessage(" ");
        sender.sendMessage("§8§m--------------------------------");
        return true;
    }


    /**
     * Reinicia los tops generales de minería/tiempo y crea una temporada
     * nueva sin procesar premios. Las temporadas cerradas y sus resultados
     * históricos se conservan.
     */
    private boolean resetSeasonAndTops(CommandSender sender) {
        if (!sender.isOp()) {
            sender.sendMessage("§cNo tienes permiso para reiniciar los tops.");
            return true;
        }

        Connection connection = null;
        boolean previousAutoCommit = true;

        try {
            connection = plugin.getDatabaseManager().getConnection();
            if (connection == null || connection.isClosed()) {
                sender.sendMessage("§cLa base de datos no está disponible.");
                return true;
            }

            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            long now = System.currentTimeMillis();
            long end = now + 30L * 24L * 60L * 60L * 1000L;

            try (PreparedStatement close = connection.prepareStatement(
                    "UPDATE seasons SET status = 'CLOSED', closed_at = ? "
                            + "WHERE status = 'ACTIVE'")) {
                close.setLong(1, now);
                close.executeUpdate();
            }

            try (PreparedStatement create = connection.prepareStatement(
                    "INSERT INTO seasons (start_time, end_time, status) "
                            + "VALUES (?, ?, 'ACTIVE')")) {
                create.setLong(1, now);
                create.setLong(2, end);
                create.executeUpdate();
            }

            try (Statement reset = connection.createStatement()) {
                reset.executeUpdate(
                        "UPDATE player_stats SET ores_mined = 0, active_seconds = 0"
                );
            }

            connection.commit();
            connection.setAutoCommit(previousAutoCommit);

            if (plugin.getPlaceholderManager() != null) {
                plugin.getPlaceholderManager().invalidateTopCache();
            }

            sender.sendMessage("§aLos tops de minería y tiempo se reiniciaron.");
            sender.sendMessage("§aSe inició una nueva temporada de 30 días.");
            sender.sendMessage("§7El historial de temporadas anteriores se conservó.");
            return true;

        } catch (SQLException exception) {
            if (connection != null) {
                try {
                    connection.rollback();
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException rollbackException) {
                    plugin.getLogger().error(
                            "No se pudo revertir el reinicio de temporada.",
                            rollbackException
                    );
                }
            }

            plugin.getLogger().error(
                    "Error reiniciando los tops y la temporada.",
                    exception
            );
            sender.sendMessage("§cNo se pudo reiniciar la temporada. Revisa la consola.");
            return true;
        }
    }

    private String formatPlaytime(long totalSeconds) {
        if (totalSeconds < 0L) {
            totalSeconds = 0L;
        }

        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (days > 0L) {
            return days + " d " + hours + " h " + minutes + " min";
        }

        if (hours > 0L) {
            return hours + " h " + minutes + " min";
        }

        return minutes + " min";
    }
}

