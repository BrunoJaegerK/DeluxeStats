package org.brunojaegerk.deluxestats;

import cn.nukkit.Player;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import me.onebone.economyapi.EconomyAPI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlaceholderManager {

    private final DeluxeStats plugin;

    private final Map<String, TopCache> topCache = new HashMap<>();

    private int topSize;
    private long topCacheTtlMs;

    public PlaceholderManager(DeluxeStats plugin) {
        this.plugin = plugin;
    }

    public void registerPlaceholders() {
        PlaceholderAPI api = PlaceholderAPI.getInstance();

        loadRankingConfig();

        // Estadísticas personales
        registerStat(api, "deluxestats_kills", "kills");
        registerStat(api, "deluxestats_deaths", "deaths");
        registerStat(api, "deluxestats_ores", "ores_mined");
        registerStat(api, "deluxestats_playtime", "active_seconds");
        registerStat(api, "deluxestats_streak", "current_streak");
        registerStat(api, "deluxestats_best_streak", "best_streak");

        // Rankings globales
        registerTopPlaceholders(api, "kills", "kills");
        registerTopPlaceholders(api, "deaths", "deaths");
        registerTopPlaceholders(api, "ores", "ores_mined");
        registerTopPlaceholders(api, "playtime", "active_seconds");
        registerTopPlaceholders(api, "streak", "current_streak");
        registerTopPlaceholders(api, "best_streak", "best_streak");

        // Saldo personal de EconomyAPI
        api.builder("deluxestats_money", String.class)
                .visitorLoader(entry -> getMoney(entry.getPlayer()))
                .build();

        plugin.getLogger().info(
                "Placeholders de DeluxeStats registrados. "
                        + "TOP configurado: " + topSize
                        + " posiciones."
        );
    }

    private void loadRankingConfig() {
        topSize = plugin.getConfig().getInt(
                "rankings.top-size", 10
        );

        topCacheTtlMs = plugin.getConfig().getInt(
                "rankings.cache-seconds", 30
        ) * 1000L;

        if (topSize < 1) {
            topSize = 10;
        } else if (topSize > 100) {
            topSize = 100;
        }

        if (topCacheTtlMs < 1000L) {
            topCacheTtlMs = 1000L;
        }
    }

    private void registerTopPlaceholders(
            PlaceholderAPI api,
            String metric,
            String column
    ) {
        for (int position = 1; position <= topSize; position++) {
            final int rank = position;

            String namePlaceholder =
                    "deluxestats_top_" + metric + "_"
                            + rank + "_name";

            String valuePlaceholder =
                    "deluxestats_top_" + metric + "_"
                            + rank + "_value";

            api.builder(namePlaceholder, String.class)
                    .loader(entry ->
                            getTopPlaceholder(
                                    metric, column, rank, "name"
                            ))
                    .build();

            api.builder(valuePlaceholder, String.class)
                    .loader(entry ->
                            getTopPlaceholder(
                                    metric, column, rank, "value"
                            ))
                    .build();
        }
    }

    private String getTopPlaceholder(
            String metric,
            String column,
            int position,
            String field
    ) {
        List<TopEntry> entries = getTopEntries(metric, column);

        int index = position - 1;

        if (index < 0 || index >= entries.size()) {
            return "name".equals(field) ? "-" : "0";
        }

        TopEntry entry = entries.get(index);

        if ("name".equals(field)) {
            return entry.name;
        }

        if ("active_seconds".equals(column)) {
            return formatPlaytime(entry.value);
        }

        return String.valueOf(entry.value);
    }

    private synchronized List<TopEntry> getTopEntries(
            String metric,
            String column
    ) {
        long now = System.currentTimeMillis();
        TopCache cached = topCache.get(metric);

        if (cached != null
                && now - cached.timestamp < topCacheTtlMs) {
            return cached.entries;
        }

        // Solo permitir las columnas conocidas del plugin.
        switch (column) {
            case "kills":
            case "deaths":
            case "ores_mined":
            case "active_seconds":
            case "current_streak":
            case "best_streak":
                break;
            default:
                return new ArrayList<>();
        }

        List<TopEntry> entries = new ArrayList<>();

        String sql = "SELECT name, " + column
                + " FROM player_stats"
                + " ORDER BY " + column + " DESC, name ASC"
                + " LIMIT " + topSize;

        try (PreparedStatement statement =
                     plugin.getDatabaseManager()
                             .getConnection()
                             .prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                entries.add(new TopEntry(
                        result.getString("name"),
                        result.getLong(column)
                ));
            }

        } catch (SQLException exception) {
            plugin.getLogger().error(
                    "Error consultando el TOP de " + metric,
                    exception
            );

            if (cached != null) {
                return cached.entries;
            }

            return entries;
        }

        topCache.put(metric, new TopCache(now, entries));
        return entries;
    }

    private void registerStat(
            PlaceholderAPI api,
            String placeholder,
            String column
    ) {
        api.builder(placeholder, String.class)
                .visitorLoader(entry -> {
                    Player player = entry.getPlayer();

                    if (player == null) {
                        return "0";
                    }

                    long value = getStat(player, column);

                    if ("active_seconds".equals(column)) {
                        return formatPlaytime(value);
                    }

                    return String.valueOf(value);
                })
                .build();
    }

    private long getStat(Player player, String column) {
        switch (column) {
            case "kills":
            case "deaths":
            case "ores_mined":
            case "active_seconds":
            case "current_streak":
            case "best_streak":
                break;
            default:
                return 0L;
        }

        String sql = "SELECT " + column
                + " FROM player_stats WHERE uuid = ?";

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        player.getUniqueId().toString()
                );

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return result.getLong(1);
                    }
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().error(
                    "Error leyendo el placeholder " + column,
                    exception
            );
        }

        return 0L;
    }

    private String getMoney(Player player) {
        if (player == null) {
            return "0";
        }

        try {
            double balance =
                    EconomyAPI.getInstance().myMoney(player);

            if (balance < 0) {
                return "0";
            }

            return String.format(
                    Locale.US,
                    "%.2f",
                    balance
            );

        } catch (Exception exception) {
            return "0";
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
            return days + " d " + hours + " h "
                    + minutes + " min";
        }

        if (hours > 0L) {
            return hours + " h " + minutes + " min";
        }

        return minutes + " min";
    }

    private static class TopEntry {
        private final String name;
        private final long value;

        private TopEntry(String name, long value) {
            this.name = name == null ? "-" : name;
            this.value = value;
        }
    }

    private static class TopCache {
        private final long timestamp;
        private final List<TopEntry> entries;

        private TopCache(long timestamp, List<TopEntry> entries) {
            this.timestamp = timestamp;
            this.entries = entries;
        }
    }
}


