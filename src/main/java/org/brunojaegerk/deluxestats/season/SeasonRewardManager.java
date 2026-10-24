package org.brunojaegerk.deluxestats.season;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.utils.Config;
import org.brunojaegerk.deluxestats.DeluxeStats;
import org.brunojaegerk.deluxestats.database.DatabaseManager;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SeasonRewardManager {

    private final DeluxeStats plugin;
    private final DatabaseManager databaseManager;
    private final Config rewardsConfig;

    public SeasonRewardManager(
            DeluxeStats plugin,
            DatabaseManager databaseManager
    ) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;

        File file = new File(plugin.getDataFolder(), "seasons.yml");

        if (!file.exists()) {
            plugin.saveResource("seasons.yml", false);
        }

        this.rewardsConfig = new Config(file, Config.YAML);
    }

    /**
     * Registra los ganadores y las recompensas configuradas.
     * No ejecuta comandos: estos se entregan después de cerrar
     * correctamente la temporada.
     */
    public void registerSeasonRewards(int seasonId) throws SQLException {
        int topSize = Math.max(
                1,
                rewardsConfig.getInt("settings.top-size", 10)
        );

        String[] categories = {
                "kills",
                "ores_mined",
                "active_seconds"
        };

        for (String category : categories) {
            List<SeasonRankingEntry> ranking =
                    databaseManager.getSeasonRanking(
                            seasonId,
                            category,
                            topSize
                    );

            for (int index = 0; index < ranking.size(); index++) {
                SeasonRankingEntry entry = ranking.get(index);
                int position = index + 1;

                databaseManager.saveSeasonWinner(
                        seasonId,
                        category,
                        position,
                        entry.getUuid(),
                        0.0
                );

                for (String command : getCommands(category, position)) {
                    long rewardId = queueCommandReward(
                            seasonId,
                            category,
                            position,
                            entry.getUuid(),
                            entry.getScore(),
                            command
                    );

                    if (rewardId > 0) {
                        plugin.getLogger().info(
                                "Recompensa registrada: temporada "
                                        + seasonId
                                        + ", categoría " + category
                                        + ", puesto " + position
                                        + ", jugador " + entry.getPlayerName()
                        );
                    }
                }
            }

            plugin.getLogger().info(
                    "Temporada " + seasonId
                            + ": procesados " + ranking.size()
                            + " puestos de " + category + "."
            );
        }
    }

    /**
     * Devuelve los comandos configurados para una categoría y puesto.
     */
    private List<String> getCommands(String category, int position) {
        List<String> commands = new ArrayList<String>();

        Object rewardsObject = rewardsConfig.get("rewards");

        if (!(rewardsObject instanceof Map)) {
            return commands;
        }

        Map<?, ?> rewards = (Map<?, ?>) rewardsObject;
        Object categoryObject = rewards.get(category);

        if (!(categoryObject instanceof Map)) {
            return commands;
        }

        Map<?, ?> categoryRewards = (Map<?, ?>) categoryObject;
        Object positionObject =
                categoryRewards.get(String.valueOf(position));

        if (!(positionObject instanceof Map)) {
            return commands;
        }

        Map<?, ?> positionRewards = (Map<?, ?>) positionObject;
        Object commandsObject = positionRewards.get("commands");

        if (!(commandsObject instanceof List)) {
            return commands;
        }

        for (Object value : (List<?>) commandsObject) {
            if (value instanceof String) {
                String command = ((String) value).trim();

                if (!command.isEmpty()) {
                    commands.add(command);
                }
            }
        }

        return commands;
    }

    /**
     * Registra un comando como recompensa pendiente en H2.
     */
    public long queueCommandReward(
            int seasonId,
            String category,
            int position,
            String uuid,
            double value,
            String command
    ) throws SQLException {

        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "El comando de recompensa no puede estar vacío."
            );
        }

        return databaseManager.createSeasonReward(
                seasonId,
                category,
                position,
                uuid,
                "COMMAND",
                value,
                command.trim()
        );
    }

    /**
     * Intenta entregar las recompensas pendientes de un jugador.
     */
    public void deliverPendingRewards(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        List<Reward> rewards;

        try {
            rewards = loadPendingRewards(
                    player.getUniqueId().toString()
            );
        } catch (SQLException exception) {
            plugin.getLogger().error(
                    "No se pudieron cargar las recompensas pendientes de "
                            + player.getName(),
                    exception
            );
            return;
        }

        for (Reward reward : rewards) {
            if (!player.isOnline()) {
                return;
            }

            boolean accepted;

            try {
                accepted = executeCommand(player, reward);
            } catch (Exception exception) {
                plugin.getLogger().error(
                        "Error al ejecutar la recompensa #" + reward.id
                                + " para " + player.getName(),
                        exception
                );
                continue;
            }

            if (!accepted) {
                continue;
            }

            try {
                if (databaseManager.claimSeasonReward(reward.id)) {
                    player.sendMessage(
                            rewardsConfig.getString(
                                    "settings.reward-message",
                                    "§a¡Has recibido una recompensa de temporada!"
                            )
                    );
                }
            } catch (SQLException exception) {
                plugin.getLogger().error(
                        "No se pudo marcar como entregada la recompensa #"
                                + reward.id,
                        exception
                );
            }
        }
    }

    /**
     * Intenta entregar las recompensas a todos los jugadores conectados.
     */
    public void deliverPendingRewardsToOnlinePlayers() {
        for (Player player :
                Server.getInstance().getOnlinePlayers().values()) {
            deliverPendingRewards(player);
        }
    }

    private boolean executeCommand(Player player, Reward reward) {
        if (!"COMMAND".equalsIgnoreCase(reward.type)) {
            plugin.getLogger().warning(
                    "Tipo de recompensa no compatible en el registro #"
                            + reward.id + ": " + reward.type
            );
            return false;
        }

        if (reward.command == null || reward.command.trim().isEmpty()) {
            return false;
        }

        String command = reward.command
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{position}", String.valueOf(reward.position))
                .replace("{season}", String.valueOf(reward.seasonId))
                .replace("{category}", reward.category)
                .replace("{value}", formatValue(reward.value))
                .trim();

        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }

        if (command.isEmpty()) {
            return false;
        }

        boolean dispatched = Server.getInstance().dispatchCommand(
                new ConsoleCommandSender(),
                command
        );

        if (!dispatched) {
            plugin.getLogger().warning(
                    "No se pudo despachar el comando de recompensa #"
                            + reward.id + ": " + command
            );
        }

        return dispatched;
    }

    private String formatValue(double value) {
        if (value == Math.rint(value)
                && value >= Long.MIN_VALUE
                && value <= Long.MAX_VALUE) {
            return String.valueOf((long) value);
        }

        return String.valueOf(value);
    }

    private List<Reward> loadPendingRewards(String uuid)
            throws SQLException {

        List<Reward> rewards = new ArrayList<Reward>();
        ResultSet result = databaseManager.getPendingSeasonRewards(uuid);
        Statement statement = null;

        try {
            statement = result.getStatement();

            while (result.next()) {
                rewards.add(new Reward(
                        result.getLong("id"),
                        result.getInt("season_id"),
                        result.getString("category"),
                        result.getInt("position"),
                        result.getString("reward_type"),
                        result.getDouble("reward_value"),
                        result.getString("reward_command")
                ));
            }
        } finally {
            try {
                result.close();
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }

        return rewards;
    }

    private static class Reward {
        private final long id;
        private final int seasonId;
        private final String category;
        private final int position;
        private final String type;
        private final double value;
        private final String command;

        private Reward(
                long id,
                int seasonId,
                String category,
                int position,
                String type,
                double value,
                String command
        ) {
            this.id = id;
            this.seasonId = seasonId;
            this.category = category;
            this.position = position;
            this.type = type;
            this.value = value;
            this.command = command;
        }
    }
}
