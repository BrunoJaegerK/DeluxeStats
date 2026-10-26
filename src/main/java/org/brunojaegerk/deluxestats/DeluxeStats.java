package org.brunojaegerk.deluxestats;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.plugin.PluginBase;

import me.onebone.economyapi.EconomyAPI;

import org.brunojaegerk.deluxestats.commands.TopStatsCommand;
import org.brunojaegerk.deluxestats.database.DatabaseManager;
import org.brunojaegerk.deluxestats.season.SeasonManager;
import org.brunojaegerk.deluxestats.season.SeasonRewardManager;
import org.brunojaegerk.deluxestats.antifarm.KillAntiFarmManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

public class DeluxeStats extends PluginBase implements Listener {

    private DatabaseManager databaseManager;
    private StatsListener statsListener;
    private PlaceholderManager placeholderManager;
    private SeasonManager seasonManager;
    private SeasonRewardManager seasonRewardManager;
    private KillAntiFarmManager killAntiFarmManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        killAntiFarmManager = new KillAntiFarmManager(this);

        try {
            // Abrir la base de datos existente sin borrar estadísticas.
            databaseManager.open();

            // Inicializar recompensas antes de iniciar las temporadas.
            seasonRewardManager = new SeasonRewardManager(
                    this,
                    databaseManager
            );

            // Registrar los eventos de esta clase.
            getServer().getPluginManager().registerEvents(this, this);

            // Registrar y mantener el seguimiento de estadísticas.
            statsListener = new StatsListener(this);

            getServer().getPluginManager().registerEvents(
                    statsListener,
                    this
            );

            statsListener.startPlaytimeTracker();

            // Iniciar el administrador de temporadas con recompensas.
            seasonManager = new SeasonManager(
                    this,
                    databaseManager,
                    seasonRewardManager
            );

            seasonManager.start();

            // Entregar recompensas pendientes a jugadores conectados.
            seasonRewardManager.deliverPendingRewardsToOnlinePlayers();

            // Registrar el comando de clasificaciones existente.
            getServer().getCommandMap().register(
                    "deluxestats",
                    new TopStatsCommand(this)
            );

            getLogger().info(
                    "DeluxeStats iniciado correctamente. H2 conectado."
            );

            getLogger().info(
                    "Estadisticas PvP, mineria, saldo y tiempo de juego activadas."
            );

            getLogger().info(
                    "Sistema de temporadas de 30 dias iniciado."
            );

        } catch (SQLException exception) {
            getLogger().error(
                    "No se pudo inicializar la base de datos H2.",
                    exception
            );

            cleanupAfterInitializationFailure();
            getServer().getPluginManager().disablePlugin(this);
            return;

        } catch (RuntimeException exception) {
            getLogger().error(
                    "Error al inicializar DeluxeStats.",
                    exception
            );

            cleanupAfterInitializationFailure();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // PlaceholderAPI es opcional.
        if (getServer().getPluginManager()
                .getPlugin("PlaceholderAPI") != null) {

            placeholderManager = new PlaceholderManager(this);
            placeholderManager.registerPlaceholders();

        } else {
            getLogger().warning(
                    "PlaceholderAPI no está instalado. "
                            + "Los placeholders no estarán disponibles."
            );
        }
    }

    /**
     * Entrega las recompensas pendientes cuando un jugador entra.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (seasonRewardManager == null) {
            return;
        }

        seasonRewardManager.deliverPendingRewards(event.getPlayer());
    }

    /**
     * Limpia los componentes inicializados si falla el arranque.
     */
    private void cleanupAfterInitializationFailure() {
        if (seasonManager != null) {
            seasonManager.stop();
            seasonManager = null;
        }

        if (statsListener != null) {
            statsListener.shutdownPlaytimeTracker();
            statsListener = null;
        }

        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!command.getName().equalsIgnoreCase("stats")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(
                    "§cEste comando solo puede usarlo un jugador."
            );
            return true;
        }

        Player player = (Player) sender;

        try {
            databaseManager.ensurePlayer(player);

            // Consultar el saldo actual en EconomyAPI.
            EconomyAPI economy = EconomyAPI.getInstance();

            if (economy == null) {
                player.sendMessage(
                        "§cEconomyAPI no está disponible."
                );
                return true;
            }

            double balance = economy.myMoney(player);

            if (balance < 0) {
                player.sendMessage(
                        "§cNo se pudo consultar tu saldo en EconomyAPI."
                );
                return true;
            }

            // Guardar el saldo actual en H2.
            databaseManager.updateMoneyBalance(player, balance);

            String sql =
                    "SELECT kills, deaths, ores_mined, money_earned, "
                            + "current_streak, best_streak, active_seconds "
                            + "FROM player_stats WHERE uuid = ?";

            Connection connection = databaseManager.getConnection();

            if (connection == null || connection.isClosed()) {
                player.sendMessage(
                        "§cLa base de datos no está disponible."
                );
                return true;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        player.getUniqueId().toString()
                );

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        player.sendMessage(
                                "§cNo se encontraron tus estadisticas."
                        );
                        return true;
                    }

                    DecimalFormat moneyFormat =
                            new DecimalFormat("#,##0.##");

                    String formattedBalance =
                            moneyFormat.format(balance);

                    long activeSeconds =
                            result.getLong("active_seconds");

                    player.sendMessage("§8§m------------------------");
                    player.sendMessage("§b§l       DELUXESTATS");
                    player.sendMessage(
                            "§7Estadisticas de §f" + player.getName()
                    );
                    player.sendMessage("§8§m------------------------");

                    player.sendMessage(
                            "§c⚔ §fAsesinatos: §a"
                                    + result.getInt("kills")
                    );

                    player.sendMessage(
                            "§c☠ §fMuertes: §a"
                                    + result.getInt("deaths")
                    );

                    player.sendMessage(
                            "§e⛏ §fMinerales picados: §a"
                                    + result.getInt("ores_mined")
                    );

                    player.sendMessage(
                            "§a$ §fSaldo actual: §e"
                                    + formattedBalance
                    );

                    player.sendMessage(
                            "§6🔥 §fRacha actual: §a"
                                    + result.getInt("current_streak")
                    );

                    player.sendMessage(
                            "§6🏆 §fMejor racha: §a"
                                    + result.getInt("best_streak")
                    );

                    player.sendMessage(
                            "§b⌛ §fTiempo jugado: §a"
                                    + formatPlaytime(activeSeconds)
                    );

                    player.sendMessage("§8§m------------------------");
                }
            }

        } catch (SQLException exception) {
            getLogger().error(
                    "Error al consultar las estadisticas de "
                            + player.getName(),
                    exception
            );

            player.sendMessage(
                    "§cNo se pudieron cargar tus estadisticas. "
                            + "Revisa la consola."
            );
        }

        return true;
    }

    private String formatPlaytime(long totalSeconds) {
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (days > 0L) {
            return days + " d " + hours + " h";
        }

        if (hours > 0L) {
            return hours + " h " + minutes + " min";
        }

        return minutes + " min";
    }

    @Override
    public void onDisable() {
        // Detener las comprobaciones de temporadas primero.
        if (seasonManager != null) {
            seasonManager.stop();
        }

        // Guardar el tiempo pendiente antes de cerrar H2.
        if (statsListener != null) {
            statsListener.shutdownPlaytimeTracker();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("DeluxeStats se ha detenido.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SeasonManager getSeasonManager() {
        return seasonManager;
    }

    public SeasonRewardManager getSeasonRewardManager() {
        return seasonRewardManager;
    }

    public KillAntiFarmManager getKillAntiFarmManager() {
        return killAntiFarmManager;
    }

}