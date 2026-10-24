package org.brunojaegerk.deluxestats.season;

import cn.nukkit.scheduler.TaskHandler;
import org.brunojaegerk.deluxestats.DeluxeStats;
import org.brunojaegerk.deluxestats.database.DatabaseManager;

import java.sql.SQLException;

public class SeasonManager {

    private static final long SEASON_DURATION =
            30L * 24L * 60L * 60L * 1000L;

    private static final int CHECK_INTERVAL_TICKS = 20 * 60;

    private final DeluxeStats plugin;
    private final DatabaseManager databaseManager;
    private final SeasonRewardManager rewardManager;

    private TaskHandler taskHandler;
    private boolean started;

    public SeasonManager(
            DeluxeStats plugin,
            DatabaseManager databaseManager,
            SeasonRewardManager rewardManager
    ) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.rewardManager = rewardManager;
    }

    public void start() {
        if (started) {
            return;
        }

        started = true;
        checkSeason();

        taskHandler = plugin.getServer()
                .getScheduler()
                .scheduleRepeatingTask(
                        plugin,
                        new Runnable() {
                            @Override
                            public void run() {
                                checkSeason();
                            }
                        },
                        CHECK_INTERVAL_TICKS
                );

        plugin.getLogger().info(
                "SeasonManager iniciado. Duración: 30 días."
        );
    }

    public synchronized void checkSeason() {
        try {
            long now = System.currentTimeMillis();
            Integer seasonId = databaseManager.getCurrentSeasonId();

            if (seasonId == null) {
                createNextSeason(now);
                return;
            }

            long endTime = databaseManager.getSeasonEndTime(seasonId);

            if (endTime > now) {
                return;
            }

            /*
             * Registrar ganadores y comandos primero.
             * Las restricciones de unicidad de H2 ayudan a evitar
             * duplicados si hay que repetir este paso tras un fallo.
             */
            rewardManager.registerSeasonRewards(seasonId);

            boolean closed = databaseManager.closeSeason(seasonId);

            if (!closed) {
                plugin.getLogger().warning(
                        "No se pudo cerrar la temporada #" + seasonId
                                + ". No se creará otra temporada."
                );
                return;
            }

            plugin.getLogger().info(
                    "Temporada #" + seasonId
                            + " cerrada correctamente."
            );

            // Las recompensas ya están persistidas en H2.
            rewardManager.deliverPendingRewardsToOnlinePlayers();

            // Comenzar la siguiente temporada de 30 días.
            createNextSeason(System.currentTimeMillis());

        } catch (SQLException exception) {
            plugin.getLogger().error(
                    "Error durante el ciclo de temporadas.",
                    exception
            );
        }
    }

    private void createNextSeason(long startTime)
            throws SQLException {

        long endTime = startTime + SEASON_DURATION;

        int seasonId = databaseManager.createSeason(
                startTime,
                endTime
        );

        plugin.getLogger().info(
                "Iniciada la temporada #" + seasonId
                        + ". Finaliza el "
                        + new java.util.Date(endTime) + "."
        );
    }

    public Integer getActiveSeasonId() throws SQLException {
        return databaseManager.getCurrentSeasonId();
    }

    public long getSeasonDuration() {
        return SEASON_DURATION;
    }

    public void stop() {
        started = false;

        if (taskHandler != null) {
            taskHandler.cancel();
            taskHandler = null;
        }

        plugin.getLogger().info("SeasonManager detenido.");
    }
}

