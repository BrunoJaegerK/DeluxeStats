
package org.brunojaegerk.deluxestats.season;

public class SeasonRankingEntry {

    private final String uuid;
    private final String playerName;
    private final long score;

    public SeasonRankingEntry(String uuid, String playerName, long score) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.score = score;
    }

    public String getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getScore() {
        return score;
    }
}

