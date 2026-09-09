package com.zpkdxgames.plexonskills.api.events;

import com.zpkdxgames.plexonskills.milestone.MilestoneDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/** Fired synchronously after a level mutation crosses a derived PlexonSkills milestone. */
public final class PlexonSkillMilestoneEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final MilestoneDefinition milestone;

    public PlexonSkillMilestoneEvent(Player player, MilestoneDefinition milestone) {
        this.player = Objects.requireNonNull(player, "player");
        this.milestone = Objects.requireNonNull(milestone, "milestone");
    }

    public Player player() { return player; }
    public MilestoneDefinition milestone() { return milestone; }
    public Player getPlayer() { return player; }
    public MilestoneDefinition getMilestone() { return milestone; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
