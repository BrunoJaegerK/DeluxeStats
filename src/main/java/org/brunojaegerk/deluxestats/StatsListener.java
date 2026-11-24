package org.brunojaegerk.deluxestats;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerChatEvent;
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

