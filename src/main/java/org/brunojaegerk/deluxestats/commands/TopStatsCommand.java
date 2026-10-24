package org.brunojaegerk.deluxestats.commands;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import org.brunojaegerk.deluxestats.DeluxeStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TopStatsCommand extends Command {

    private final DeluxeStats plugin;

    public TopStatsCommand(DeluxeStats plugin) {
        super(
                "topstats",
                "Muestra los mejores jugadores del servidor",
                "/topstats <kills|deaths|ores|playtime|streak>"
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
            sender.sendMessage("§b§lDeluxeStats §8» §fUso: §a/topstats <kills|deaths|ores|playtime|streak>");
            return true;
        }

        String type = args[0].toLowerCase(java.util.Locale.ROOT);

        String column;
        String title;
        String valueLabel;

        switch (type) {
            case "kills":
                column = "kills";
                title = "Más asesinatos";
                valueLabel = "asesinatos";
                break;

            case "deaths":
                column = "deaths";
                title = "Más muertes";
                valueLabel = "muertes";
                break;

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

            case "streak":
                column = "best_streak";
                title = "Mejores rachas";
                valueLabel = "racha";
                break;

            default:
                sender.sendMessage("§b§lDeluxeStats §8» §cCategoría desconocida.");
                sender.sendMessage("§7Categorías: §akills, deaths, ores, playtime, streak");
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

