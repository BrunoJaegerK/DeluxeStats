package org.brunojaegerk.deluxestats;

import cn.nukkit.plugin.PluginBase;
import org.brunojaegerk.deluxestats.database.DatabaseManager;

import java.sql.SQLException;

public class DeluxeStats extends PluginBase {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(this);

        try {
            databaseManager.open();

            getServer().getPluginManager().registerEvents(
                    new StatsListener(this),
                    this
            );

            getLogger().info(
                    "DeluxeStats iniciado correctamente. SQLite conectado."
            );
            getLogger().info("Estadísticas PvP y minería activadas.");

        } catch (SQLException exception) {
            getLogger().error(
                    "No se pudo inicializar SQLite.",
                    exception
            );
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("DeluxeStats se ha detenido.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}

