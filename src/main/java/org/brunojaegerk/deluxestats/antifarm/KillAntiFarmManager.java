package org.brunojaegerk.deluxestats.antifarm;

import cn.nukkit.Player;
import org.brunojaegerk.deluxestats.DeluxeStats;
import org.brunojaegerk.deluxestats.database.DatabaseManager;

import java.sql.SQLException;

public class KillAntiFarmManager {

    // 2 minutos y 30 segundos.
    private static final long COOLDOWN_MS = 150_000L;

    private final DatabaseManager databaseManager;

    public KillAntiFarmManager(DeluxeStats plugin) {
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * @return 0 si la kill puede puntuar;
     *         milisegundos restantes si está bloqueada.
     */
    public long getRemainingCooldownMillis(
            Player killer,
            Player victim
    ) throws SQLException {

        if (killer == null || victim == null) {
            return COOLDOWN_MS;
        }

        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return COOLDOWN_MS;
        }

        if (databaseManager == null) {
            throw new SQLException(
                    "DatabaseManager no está inicializado."
            );
        }

        return databaseManager.tryRegisterKillPair(
                killer,
                victim,
                System.currentTimeMillis(),
                COOLDOWN_MS
        );
    }
}
