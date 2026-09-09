package com.zpkdxgames.plexonskills.api.events;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public final class PlexonSkillXpGainEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final SkillType skill;
    private final long amount;
    private final String source;
    private final long projectedTotalXp;
    private final int currentLevel;
    private final long transactionId;
    private boolean cancelled;

    public PlexonSkillXpGainEvent(Player player, SkillType skill, long amount, String source, long projectedTotalXp, int currentLevel, long transactionId) {
        this.player = Objects.requireNonNull(player);
        this.skill = Objects.requireNonNull(skill);
        this.amount = amount;
        this.source = Objects.requireNonNullElse(source, "UNKNOWN");
        this.projectedTotalXp = projectedTotalXp;
        this.currentLevel = currentLevel;
        this.transactionId = transactionId;
    }
    public Player player() { return player; }
    public SkillType skill() { return skill; }
    public long amount() { return amount; }
    public String source() { return source; }
    public long projectedTotalXp() { return projectedTotalXp; }
    public int currentLevel() { return currentLevel; }
    public long transactionId() { return transactionId; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
