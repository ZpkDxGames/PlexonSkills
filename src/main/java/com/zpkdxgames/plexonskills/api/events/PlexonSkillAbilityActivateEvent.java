package com.zpkdxgames.plexonskills.api.events;

import com.zpkdxgames.plexonskills.skill.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/** Synchronous cancellable event fired immediately before an ability becomes active. */
public final class PlexonSkillAbilityActivateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player; private final SkillType skill; private final String abilityId; private boolean cancelled;
    public PlexonSkillAbilityActivateEvent(Player player, SkillType skill, String abilityId) { this.player=Objects.requireNonNull(player); this.skill=Objects.requireNonNull(skill); this.abilityId=Objects.requireNonNull(abilityId); }
    public Player player(){return player;} public SkillType skill(){return skill;} public String abilityId(){return abilityId;}
    public Player getPlayer(){return player;} public SkillType getSkill(){return skill;} public String getAbilityId(){return abilityId;}
    @Override public boolean isCancelled(){return cancelled;} @Override public void setCancelled(boolean value){cancelled=value;}
    @Override public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
