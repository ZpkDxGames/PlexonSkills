package com.zpkdxgames.plexonskills.api.events;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public final class PlexonSkillLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final SkillType skill;
    private final int oldLevel;
    private final int newLevel;
    private final long totalXp;
    private final String source;

    public PlexonSkillLevelUpEvent(Player player, SkillType skill, int oldLevel, int newLevel, long totalXp, String source) {
        this.player = Objects.requireNonNull(player); this.skill = Objects.requireNonNull(skill);
        this.oldLevel = oldLevel; this.newLevel = newLevel; this.totalXp = totalXp; this.source = Objects.requireNonNullElse(source, "UNKNOWN");
    }
    public Player player() { return player; } public SkillType skill() { return skill; }
    public int oldLevel() { return oldLevel; } public int newLevel() { return newLevel; }
    public long totalXp() { return totalXp; } public String source() { return source; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
