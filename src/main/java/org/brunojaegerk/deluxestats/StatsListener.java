package org.brunojaegerk.deluxestats;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.PluginBase;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class StatsListener implements Listener {

    private static final long AFK_TIMEOUT_MS = 60_000L;
    private static final long SAVE_INTERVAL_MS = 60_000L;

    private final PluginBase plugin;
    private final DeluxeStats deluxeStats;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public StatsListener(DeluxeStats plugin) {
        this.plugin = plugin;
        this.deluxeStats = plugin;
    }

    public void startPlaytimeTracker() {
        long now = System.currentTimeMillis();

        for (Player player :
                plugin.getServer().getOnlinePlayers().values()) {
            sessions.put(
                    player.getUniqueId(),
                    new Session(player, now)
            );
        }

        plugin.getServer().getScheduler().scheduleRepeatingTask(
                plugin,
                new Runnable() {
                    @Override
                    public void run() {
                        updatePlaytime();
                    }
                },
                20
        );
    }

    private void updatePlaytime() {
        long now = System.currentTimeMillis();

        for (Player player :
                plugin.getServer().getOnlinePlayers().values()) {

            UUID uuid = player.getUniqueId();
            Session session = sessions.get(uuid);

            if (session == null) {
                sessions.put(uuid, new Session(player, now));
                continue;
            }

            accrueElapsed(session, now);

            if (session.pendingMillis >= SAVE_INTERVAL_MS) {
                flushSession(session);
            }
        }
    }

    /**
     * Cuenta únicamente el intervalo que cae dentro de los
     * primeros 60 segundos posteriores a la última actividad.
     */
    private void accrueElapsed(Session session, long now) {
        long intervalStart = session.lastSampleMillis;
        long intervalEnd = Math.max(intervalStart, now);
        long activeUntil =
                session.lastActivityMillis + AFK_TIMEOUT_MS;

        long countedUntil = Math.min(intervalEnd, activeUntil);
        long activeMillis =
                Math.max(0L, countedUntil - intervalStart);

        session.pendingMillis += activeMillis;
        session.lastSampleMillis = intervalEnd;
    }

    private void markActivity(Player player) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        Session session = sessions.get(uuid);

        if (session == null) {
            sessions.put(uuid, new Session(player, now));
            return;
        }

        // Guardar el intervalo anterior antes de reiniciar el AFK.
        accrueElapsed(session, now);

        session.lastActivityMillis = now;
        session.lastSampleMillis = now;

        if (session.pendingMillis >= SAVE_INTERVAL_MS) {
            flushSession(session);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        sessions.put(
                player.getUniqueId(),
                new Session(player, now)
        );
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom() == null || event.getTo() == null) {
            return;
        }

        boolean positionChanged =
                event.getFrom().getX() != event.getTo().getX()
                        || event.getFrom().getY() != event.getTo().getY()
                        || event.getFrom().getZ() != event.getTo().getZ();

        boolean lookingChanged =
                event.getFrom().getYaw() != event.getTo().getYaw()
                        || event.getFrom().getPitch()
                        != event.getTo().getPitch();

        if (positionChanged || lookingChanged) {
            markActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        markActivity(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        markActivity(event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (event.isCancelled()) {
            return;
        }

        markActivity(player);

        Block block = event.getBlock();
        String blockName =
                block.getName().toLowerCase(Locale.ROOT);

        if (!blockName.contains("ore")) {
            return;
        }

        try {
            deluxeStats.getDatabaseManager().recordOre(player);
        } catch (SQLException exception) {
            plugin.getLogger().error(
                    "Error al guardar los minerales extraídos.",
                    exception
            );
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = (Player) event.getEntity();

        try {
            deluxeStats.getDatabaseManager().recordDeath(victim);

            Entity lastDamage =
                    victim.getLastDamageCause()
                            instanceof EntityDamageByEntityEvent
                            ? ((EntityDamageByEntityEvent)
                            victim.getLastDamageCause()).getDamager()
                            : null;

            if (lastDamage instanceof Player) {
                Player killer = (Player) lastDamage;

                if (!killer.getUniqueId().equals(
                        victim.getUniqueId())) {

                    deluxeStats.getDatabaseManager().recordKill(killer);
                    markActivity(killer);
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().error(
                    "Error al guardar las estadísticas PvP.",
                    exception
            );
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Session session = sessions.remove(player.getUniqueId());

        if (session != null) {
            // Incluye solo el intervalo activo hasta la desconexión.
            accrueElapsed(session, System.currentTimeMillis());
            flushSession(session);
        }
    }

    private void flushSession(Session session) {
        long wholeSeconds = session.pendingMillis / 1000L;

        if (wholeSeconds <= 0L) {
            return;
        }

        try {
            deluxeStats.getDatabaseManager().addActiveSeconds(
                    session.player,
                    wholeSeconds
            );

            session.pendingMillis -= wholeSeconds * 1000L;
        } catch (SQLException exception) {
            plugin.getLogger().error(
                    "Error al guardar el tiempo de juego de "
                            + session.player.getName(),
                    exception
            );
        }
    }

    public void shutdownPlaytimeTracker() {
        long now = System.currentTimeMillis();

        for (Session session : sessions.values()) {
            accrueElapsed(session, now);
            flushSession(session);
        }

        sessions.clear();
    }

    private static class Session {
        private final Player player;
        private long lastActivityMillis;
        private long lastSampleMillis;
        private long pendingMillis;

        private Session(Player player, long now) {
            this.player = player;
            this.lastActivityMillis = now;
            this.lastSampleMillis = now;
            this.pendingMillis = 0L;
        }
    }
}

